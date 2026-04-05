package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.CaCfirTopLevelPublicSymbolQueryValue
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFileSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile

/**
 * 公开符号缓存键。
 *
 * 同一语义符号在同一 session 内必须只暴露一个公开实例，
 * 因此缓存键只表达稳定语义身份，不容纳任何兜底推断。
 */
internal sealed interface CaCfirPublicSymbolCacheKey

internal data class CaCfirPackageSymbolCacheKey(
    val fqName: FqName,
) : CaCfirPublicSymbolCacheKey

internal data class CaCfirFileSymbolCacheKey(
    val file: CjFile,
) : CaCfirPublicSymbolCacheKey

internal data class CaCfirClassLikeSymbolCacheKey(
    val classId: ClassId,
) : CaCfirPublicSymbolCacheKey

internal data class CaCfirCallableSymbolCacheKey(
    val callableId: CallableId,
) : CaCfirPublicSymbolCacheKey

/**
 * 补全判定缓存使用的符号键。
 *
 * 具备稳定公开身份的符号直接复用公开缓存键；
 * 只有不属于公开恢复协议的临时符号才允许退回 session 内实例身份。
 */
internal sealed interface CaCfirCompletionSymbolKey

internal data class CaCfirStableCompletionSymbolKey(
    val symbolKey: CaCfirPublicSymbolCacheKey,
) : CaCfirCompletionSymbolKey

internal data class CaCfirEphemeralCompletionSymbolKey(
    val symbol: CaSymbol,
) : CaCfirCompletionSymbolKey

/**
 * `analysis-api-cfir` 内部统一的 low-level -> public symbol 工厂入口。
 */
internal fun CaCfirSession.getPublicSymbol(symbol: CfirSymbol<*>): CaSymbol = when (symbol) {
    is CfirClassLikeSymbol<*> -> createClassLikeSymbol(symbol)
    is CfirCallableSymbol<*> -> createCallableSymbol(symbol)
    is CfirFileSymbol -> createFilePublicSymbol(symbol)
    else -> error("暂不支持将 ${symbol::class.simpleName} 映射为公开 CaSymbol")
}

internal fun CaCfirSession.getPackagePublicSymbol(fqName: FqName): CaPackageSymbol? {
    if (!hasVisiblePackage(fqName)) return null
    return createPackageSymbol(fqName)
}

internal fun CaCfirSession.getClassLikePublicSymbol(classId: ClassId): CaClassLikeSymbol? {
    val symbol = lookupClassLikeSymbol(classId) ?: return null
    return createClassLikeSymbol(symbol)
}

internal fun CaCfirSession.createFilePublicSymbol(symbol: CfirFileSymbol): CaFileSymbol {
    val file = lookupContainingFile(symbol)
        ?: error("文件符号缺少可恢复的 CjFile 源：$symbol")
    return createFileSymbol(file)
}

/**
 * 统一恢复同一包和短名下的顶层公开符号。
 */
internal fun CaCfirSession.getOrCreateTopLevelPublicSymbols(
    packageFqName: FqName,
    name: Name,
): CaCfirTopLevelPublicSymbolQueryValue {
    return getOrCreateTopLevelSymbolQuery(packageFqName, name) {
        val queryResult = queryTopLevelSymbols(packageFqName, name)
        CaCfirTopLevelPublicSymbolQueryValue(
            classLikeSymbols = queryResult.classLikeSymbols.map(::createClassLikeSymbol),
            callableSymbols = queryResult.callableSymbols.map(::createCallableSymbol),
        )
    }
}

internal fun CaCfirSession.createPackageSymbol(fqName: FqName): CaPackageSymbol =
    getOrCreatePublicSymbol(CaCfirPackageSymbolCacheKey(fqName)) {
        CaCfirPackageSymbolImpl(
            fqName = fqName,
            containingModule = useSiteModule,
            token = token,
        )
    }

internal fun CaCfirSession.createFileSymbol(file: CjFile): CaFileSymbol =
    getOrCreatePublicSymbol(CaCfirFileSymbolCacheKey(file)) {
        createUncachedFileSymbol(file)
    }

private fun CaCfirSession.createUncachedFileSymbol(file: CjFile): CaFileSymbol {
    val fileSymbol = lookupFileSymbol(file)
        ?: error("无法为 `${file.name}` 构建 low-level 文件符号")
    return CaCfirFileSymbolImpl(
        backingSymbol = fileSymbol,
        file = file,
        containingModule = useSiteModule,
        token = token,
    )
}

internal fun CaCfirSession.createClassLikeSymbol(symbol: CfirClassLikeSymbol<*>): CaClassLikeSymbol =
    getOrCreatePublicSymbol(CaCfirClassLikeSymbolCacheKey(symbol.classId)) {
        CaCfirClassLikeSymbolImpl(
            backingSymbol = symbol,
            analysisSession = this,
            containingModule = useSiteModule,
            token = token,
        )
    }

internal fun CaCfirSession.createCallableSymbol(symbol: CfirCallableSymbol<*>): CaCallableSymbol =
    getOrCreatePublicSymbol(symbol.publicSymbolCacheKey()) {
        CaCfirCallableSymbolImpl(
            backingSymbol = symbol,
            analysisSession = this,
            containingModule = useSiteModule,
            token = token,
        )
    }

private fun CfirCallableSymbol<*>.publicSymbolCacheKey(): CaCfirCallableSymbolCacheKey =
    CaCfirCallableSymbolCacheKey(callableId)

/**
 * 恢复 callable 符号时，顶层 callable 与成员 callable 必须走不同的公开协议。
 */
internal fun CaCfirSession.restoreCallablePublicSymbol(callableId: CallableId): CaCallableSymbol? {
    val ownerClassId = callableId.classId
    val candidates = when (ownerClassId) {
        null -> getOrCreateTopLevelPublicSymbols(
            packageFqName = callableId.packageName,
            name = callableId.callableName,
        ).callableSymbols

        else -> queryDeclaredMemberScope(ownerClassId)
            ?.getCallableSymbols(callableId.callableName)
            ?.map(::createCallableSymbol)
            .orEmpty()
    }
    return candidates.singleOrNull()
}

/**
 * 为补全缓存和作用域去重提供稳定语义键。
 */
internal fun CaSymbol.completionDecisionKey(): CaCfirCompletionSymbolKey =
    publicSymbolCacheKeyOrNull()
        ?.let(::CaCfirStableCompletionSymbolKey)
        ?: CaCfirEphemeralCompletionSymbolKey(this)
