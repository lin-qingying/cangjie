package org.cangnova.cangjie.analysis.api.cfir.symbols

import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaNamedFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySymbol
import org.cangnova.cangjie.analysis.low.level.api.cfir.providers.CfirCallableSignature
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.session.extendIndexStore
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.Name

/**
 * 按 callableId、callable kind 与签名恢复稳定的公开 callable 符号。
 *
 * [signature] 是区分同名重载的必要条件：没有它，两个重载都会通过筛选，
 * `singleOrNull` 因「命中多个」返回 `null`，两个符号一起丢失。
 */
internal fun CaCfirSession.restoreCallablePublicSymbol(
    callableId: CallableId,
    kind: CaCfirCallableSymbolKind,
    signature: CfirCallableSignature?,
): CaCallableSymbol? {
    val ownerClassId = callableId.classId
    val candidates = when (ownerClassId) {
        null -> getTopLevelCallableSymbols(callableId.packageName, callableId.callableName)
        else -> {
            if (kind == CaCfirCallableSymbolKind.ENUM_CONSTRUCTOR) {
                val ownerEnum = cfirSession.symbolProvider.getClassLikeSymbolByClassId(ownerClassId)?.cfir
                    as? org.cangnova.cangjie.cfir.declarations.CfirEnum ?: return null
                return ownerEnum.declarations.filterIsInstance<org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor>()
                    .filter { it.name == callableId.callableName }
                    .map { cfirSymbolBuilder.buildSymbol(it.symbol) as CaCallableSymbol }
                    .singleOrNull { it.matchesStableCallable(callableId, kind, signature) }
            }
            val ownerClass = cfirSession.symbolProvider.getClassLikeSymbolByClassId(ownerClassId)?.cfir as? CfirClass
                ?: return null
            cfirSession.cangjieScopeProvider.getDeclarationSiteMemberScope(
                ownerClass,
                cfirSession,
                getScopeSessionFor(cfirSession),
            ).let { scope ->
                buildList {
                    scope.processCallablesByName(callableId.callableName) { symbol ->
                        add(cfirSymbolBuilder.buildSymbol(symbol) as CaCallableSymbol)
                    }
                }
            }
        }
    }
    val stableCandidate = candidates.singleOrNull { candidate ->
        candidate.matchesStableCallable(callableId, kind, signature)
    }
    if (stableCandidate != null) return stableCandidate

    return null
}

/**
 * 顶层命名函数恢复必须带 signature。
 *
 * 这是 Kotlin `KaFirTopLevelFunctionSymbolPointer` 在仓颉侧的等价恢复入口，
 * 用来区分同名重载函数，避免把顶层函数退化成 `CallableId -> singleOrNull`。
 */
@OptIn(CaImplementationDetail::class)
internal fun CaCfirSession.restoreTopLevelFunctionPublicSymbol(
    callableId: CallableId,
    signature: CfirCallableSignature,
): CaNamedFunctionSymbol? {
    val candidates = cfirSession.symbolProvider.getTopLevelCallableSymbols(callableId.packageName, callableId.callableName)
    val functionSymbol = candidates
        .filterIsInstance<CfirNamedFunctionSymbol>()
        .singleOrNull { candidate -> signature.hasTheSameSignature(candidate) }
        ?: return null
    return cfirSymbolBuilder.functionBuilder.buildNamedFunctionSymbol(functionSymbol)
}

/**
 * 按 extend 稳定身份、成员 callable 信息与签名恢复 extend 成员公开符号。
 */
internal fun CaCfirSession.restoreExtendMemberCallablePublicSymbol(
    extendIdentity: CaCfirExtendSymbolIdentity,
    callableName: Name,
    kind: CaCfirCallableSymbolKind,
    signature: CfirCallableSignature?,
): CaCallableSymbol? {
    val extendSymbol = restoreExtendPublicSymbol(extendIdentity) ?: return null
    val candidates = with(this) {
        extendSymbol.declaredMemberScope.callables(callableName).toList()
    }
    val expectedKey = CaCfirExtendMemberCallableSymbolCacheKey(extendIdentity, callableName, kind, signature)
    return candidates.singleOrNull { candidate ->
        candidate.publicSymbolCacheKeyOrNull() == expectedKey
    }
}

/**
 * 按稳定 extend identity 恢复公开 extend 符号。
 */
internal fun CaCfirSession.restoreExtendPublicSymbol(
    extendIdentity: CaCfirExtendSymbolIdentity,
): org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol? {
    val extendDeclaration = cfirSession.extendIndexStore
        .modelsInPackage(extendIdentity.packageFqName)
        .firstOrNull { model -> model.toPublicSymbolIdentity() == extendIdentity }
        ?.declaration
        ?: return null
    return cfirSymbolBuilder.buildExtendSymbol(extendDeclaration.symbol)
}


/**
 * 判断公开 callable 符号是否匹配稳定缓存键中的 callableId、kind 与签名。
 *
 * 签名比较在 [signature] 为 `null` 时跳过：那种键对应没有 callable 声明形态的符号，
 * 本来就不存在同名重载，跳过不会造成合并。
 */
private fun CaCallableSymbol.matchesStableCallable(
    callableId: CallableId,
    kind: CaCfirCallableSymbolKind,
    signature: CfirCallableSignature?,
): Boolean {
    if (this.callableId != callableId) return false
    if (signature != null) {
        val cfirCallable = (this as? CaCfirSymbol<*>)?.cfirSymbolOrNull as? CfirCallableSymbol<*> ?: return false
        if (!signature.hasTheSameSignature(cfirCallable)) return false
    }
    return when (kind) {
        CaCfirCallableSymbolKind.NAMED_FUNCTION -> this is CaNamedFunctionSymbol &&
            this !is org.cangnova.cangjie.analysis.api.symbols.CaMainFunctionSymbol &&
            this !is org.cangnova.cangjie.analysis.api.symbols.CaMacroSymbol

        CaCfirCallableSymbolKind.MAIN_FUNCTION -> this is org.cangnova.cangjie.analysis.api.symbols.CaMainFunctionSymbol
        CaCfirCallableSymbolKind.MACRO -> this is org.cangnova.cangjie.analysis.api.symbols.CaMacroSymbol
        CaCfirCallableSymbolKind.FINALIZER -> this is org.cangnova.cangjie.analysis.api.symbols.CaFinalizerSymbol
        CaCfirCallableSymbolKind.CONSTRUCTOR -> this is org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
        CaCfirCallableSymbolKind.PROPERTY -> this is CaPropertySymbol
        CaCfirCallableSymbolKind.FIELD -> this is org.cangnova.cangjie.analysis.api.symbols.CaFieldSymbol
        CaCfirCallableSymbolKind.PATTERN_BINDING -> this is org.cangnova.cangjie.analysis.api.symbols.CaPatternBindingSymbol
        CaCfirCallableSymbolKind.ENUM_CONSTRUCTOR -> this is org.cangnova.cangjie.analysis.api.symbols.CaEnumConstructorSymbol
    }
}
