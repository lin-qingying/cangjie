package org.cangnova.cangjie.analysis.api.cfir.symbols

import org.cangnova.cangjie.analysis.api.cfir.*

import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaNamedFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.low.level.api.cfir.providers.CfirCallableSignature
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.cfir.symbols.CfirAnonymousFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFieldVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFinalizerSymbol
import org.cangnova.cangjie.cfir.symbols.CfirMacroDeclarationSymbol
import org.cangnova.cangjie.cfir.symbols.CfirMainFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternBindingSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.extendIndexStore
import org.cangnova.cangjie.cfir.resolve.providers.CfirProviderImpl
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.Name

/**
 * 按 callableId 与 callable kind 恢复稳定的公开 callable 符号。
 */
internal fun CaCfirSession.restoreCallablePublicSymbol(
    callableId: CallableId,
    kind: CaCfirCallableSymbolKind,
): CaCallableSymbol? {
    val ownerClassId = callableId.classId
    val candidates = when (ownerClassId) {
        null -> getTopLevelCallableSymbols(callableId.packageName, callableId.callableName)
        else -> {
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
    val stableCandidate = candidates.singleOrNull { candidate -> candidate.matchesStableCallable(callableId, kind) }
    if (stableCandidate != null) return stableCandidate

    if (kind == CaCfirCallableSymbolKind.PATTERN_VARIABLE || kind == CaCfirCallableSymbolKind.PATTERN_BINDING) {
        return ((runCatching { cfirSession.cfirProvider }.getOrNull() as? CfirProviderImpl)
            ?.getAllFiles()
            ?.asSequence()
            ?.mapNotNull { file -> file.findCallableSymbol(callableId, kind) }
            ?.firstOrNull())
            ?.let { cfirSymbolBuilder.buildSymbol(it) as CaCallableSymbol }
    }

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
 * 按 extend 稳定身份和成员 callable 信息恢复 extend 成员公开符号。
 */
internal fun CaCfirSession.restoreExtendMemberCallablePublicSymbol(
    extendIdentity: CaCfirExtendSymbolIdentity,
    callableName: Name,
    kind: CaCfirCallableSymbolKind,
): CaCallableSymbol? {
    val extendSymbol = restoreExtendPublicSymbol(extendIdentity) ?: return null
    val candidates = with(this) {
        extendSymbol.declaredMemberScope.callables(callableName).toList()
    }
    val expectedKey = CaCfirExtendMemberCallableSymbolCacheKey(extendIdentity, callableName, kind)
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
 * 在单个 CFIR 文件树中查找匹配 callableId 与 kind 的 callable 符号。
 */
private fun CfirFile.findCallableSymbol(
    callableId: CallableId,
    kind: CaCfirCallableSymbolKind,
): CfirCallableSymbol<*>? {
    var result: CfirCallableSymbol<*>? = null
    accept(object : CfirVisitorVoid() {
        override fun visitElement(element: org.cangnova.cangjie.cfir.CfirElement) {
            if (result != null) return
            when (element) {
                is CfirCallableDeclaration -> {
                    val symbol = element.symbol
                    if (symbol.callableId == callableId && symbol.matchesKind(kind)) {
                        result = symbol
                        return
                    }
                }
            }
            element.acceptChildren(this)
        }
    }, null)
    return result
}

/**
 * 判断底层 CFIR callable 符号是否属于指定公开 callable kind。
 */
private fun CfirCallableSymbol<*>.matchesKind(kind: CaCfirCallableSymbolKind): Boolean = when (kind) {
    CaCfirCallableSymbolKind.PATTERN_VARIABLE -> this is CfirPatternVariableSymbol
    CaCfirCallableSymbolKind.PATTERN_BINDING -> this is CfirPatternBindingSymbol
    CaCfirCallableSymbolKind.NAMED_FUNCTION -> this is CfirNamedFunctionSymbol
    CaCfirCallableSymbolKind.MAIN_FUNCTION -> this is CfirMainFunctionSymbol
    CaCfirCallableSymbolKind.MACRO -> this is CfirMacroDeclarationSymbol
    CaCfirCallableSymbolKind.FINALIZER -> this is CfirFinalizerSymbol
    CaCfirCallableSymbolKind.CONSTRUCTOR -> this is CfirConstructorSymbol
    CaCfirCallableSymbolKind.PROPERTY -> this is CfirPropertySymbol
    CaCfirCallableSymbolKind.FIELD -> this is CfirFieldVariableSymbol
    CaCfirCallableSymbolKind.ENUM_CONSTRUCTOR -> this is CfirEnumConstructorSymbol
}

/**
 * 判断公开 callable 符号是否匹配稳定缓存键中的 callableId 与 kind。
 */
private fun CaCallableSymbol.matchesStableCallable(
    callableId: CallableId,
    kind: CaCfirCallableSymbolKind,
): Boolean {
    if (this.callableId != callableId) return false
    return when (kind) {
        CaCfirCallableSymbolKind.NAMED_FUNCTION -> this is org.cangnova.cangjie.analysis.api.symbols.CaNamedFunctionSymbol &&
            this !is org.cangnova.cangjie.analysis.api.symbols.CaMainFunctionSymbol &&
            this !is org.cangnova.cangjie.analysis.api.symbols.CaMacroSymbol

        CaCfirCallableSymbolKind.MAIN_FUNCTION -> this is org.cangnova.cangjie.analysis.api.symbols.CaMainFunctionSymbol
        CaCfirCallableSymbolKind.MACRO -> this is org.cangnova.cangjie.analysis.api.symbols.CaMacroSymbol
        CaCfirCallableSymbolKind.FINALIZER -> this is org.cangnova.cangjie.analysis.api.symbols.CaFinalizerSymbol
        CaCfirCallableSymbolKind.CONSTRUCTOR -> this is org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
        CaCfirCallableSymbolKind.PROPERTY -> this is CaPropertySymbol
        CaCfirCallableSymbolKind.FIELD -> this is org.cangnova.cangjie.analysis.api.symbols.CaFieldSymbol
        CaCfirCallableSymbolKind.PATTERN_VARIABLE -> this is org.cangnova.cangjie.analysis.api.symbols.CaPatternVariableSymbol
        CaCfirCallableSymbolKind.PATTERN_BINDING -> this is org.cangnova.cangjie.analysis.api.symbols.CaPatternBindingSymbol
        CaCfirCallableSymbolKind.ENUM_CONSTRUCTOR -> this is org.cangnova.cangjie.analysis.api.symbols.CaEnumConstructorSymbol
    }
}
