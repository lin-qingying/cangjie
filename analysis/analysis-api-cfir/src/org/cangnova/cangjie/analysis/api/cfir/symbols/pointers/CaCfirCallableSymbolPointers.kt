package org.cangnova.cangjie.analysis.api.cfir.symbols.pointers

import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirCallableSymbolCacheKey
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirExtendMemberCallableSymbolCacheKey
import org.cangnova.cangjie.analysis.api.cfir.symbols.restoreCallablePublicSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.restoreExtendMemberCallablePublicSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaNamedFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.low.level.api.cfir.providers.CfirCallableSignature
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.name.CallableId

/**
 * 具备稳定 callable 标识的专用 pointer。
 *
 * 仓颉当前公开 callable 恢复入口已经由 `CallableId + kind` 和
 * `extendId + callableName + kind` 两组稳定身份覆盖，这里直接使用它们构建 pointer。
 */
internal class CaCfirCallableSymbolPointer<S : CaCallableSymbol>(
    private val cacheKey: CaCfirCallableSymbolCacheKey,
    private val symbolType: Class<S>,
) : CaCfirSymbolPointerBase<S>() {
    override fun restoreSymbol(session: org.cangnova.cangjie.analysis.api.CaSession): S? =
        symbolType.castOrNull(
            restoreSession(session)?.restoreCallablePublicSymbol(
                callableId = cacheKey.callableId,
                kind = cacheKey.kind,
            )
        )
}

internal class CaCfirExtendMemberCallableSymbolPointer<S : CaCallableSymbol>(
    private val cacheKey: CaCfirExtendMemberCallableSymbolCacheKey,
    private val symbolType: Class<S>,
) : CaCfirSymbolPointerBase<S>() {
    override fun restoreSymbol(session: org.cangnova.cangjie.analysis.api.CaSession): S? =
        symbolType.castOrNull(
            restoreSession(session)?.restoreExtendMemberCallablePublicSymbol(
                extendIdentity = cacheKey.extendIdentity,
                callableName = cacheKey.callableName,
                kind = cacheKey.kind,
            )
        )
}

/**
 * 对齐 Kotlin `KaTopLevelCallableSymbolPointer` 的仓颉侧基座。
 *
 * 顶层 callable pointer 的职责是：
 * 1. 先基于 `CallableId` 收集顶层候选；
 * 2. 再由具体子类按 signature / declaration kind 选择唯一目标；
 * 3. 最终构造 public symbol。
 */
@OptIn(CaImplementationDetail::class)
internal abstract class CaTopLevelCallableSymbolPointer<S : CaCallableSymbol>(
    private val callableId: CallableId,
    originalSymbol: S?,
) : CaCfirCachedSymbolPointer<S>(originalSymbol) {
    final override fun restoreIfNotCached(analysisSession: CaSession): S? {
        require(analysisSession is CaCfirSession)
        val candidates = analysisSession.getCallableSymbols(callableId)
        if (candidates.isEmpty()) return null
        val session = candidates.first().cfir.moduleData.session
        return analysisSession.chooseCandidateAndCreateSymbol(candidates, session)
    }


    protected abstract fun CaCfirSession.chooseCandidateAndCreateSymbol(
        candidates: Collection<CfirCallableSymbol<*>>,
        cfirSession: CfirSession
    ): S?
    protected fun hasTheSameOwner(other: CaTopLevelCallableSymbolPointer<*>): Boolean = other.callableId == callableId
}

/**
 * 顶层命名函数指针。
 *
 * 与 Kotlin FIR 一样，真正区分重载的是 callable signature，
 * 不是 `CallableId` 本身。
 */
@OptIn(CaImplementationDetail::class)
internal class CaCfirTopLevelFunctionSymbolPointer(
    callableId: CallableId,
    private val signature: CfirCallableSignature,
    originalSymbol: CaNamedFunctionSymbol?,
) : CaTopLevelCallableSymbolPointer<CaNamedFunctionSymbol>(callableId, originalSymbol) {
    override fun CaCfirSession.chooseCandidateAndCreateSymbol(
        candidates: Collection<CfirCallableSymbol<*>>,
        cfirSession: CfirSession
    ): CaNamedFunctionSymbol? {
        val function = candidates.findDeclarationWithSignatureBySymbols<CfirNamedFunction>(signature) ?: return null
        return cfirSymbolBuilder.functionBuilder.buildNamedFunctionSymbol(function.symbol)

    }

    fun pointsToTheSameSymbolAs(other: CaSymbolPointer<CaSymbol>): Boolean = this === other ||
        other is CaCfirTopLevelFunctionSymbolPointer &&
        other.signature == signature &&
        hasTheSameOwner(other)
}

@OptIn(CaImplementationDetail::class)
internal inline fun <reified D : CfirCallableDeclaration> Collection<CfirCallableSymbol<*>>.findDeclarationWithSignatureBySymbols(
    signature: CfirCallableSignature,
): D? {
    for (symbol in this) {
        val declaration = symbol.cfir
        if (declaration is D && signature.hasTheSameSignature(declaration)) {
            return declaration
        }
    }
    return null
}

internal fun CaCfirSession.getCallableSymbols(callableId: CallableId) =
    cfirSession.symbolProvider.getTopLevelCallableSymbols(callableId.packageName, callableId.callableName)
