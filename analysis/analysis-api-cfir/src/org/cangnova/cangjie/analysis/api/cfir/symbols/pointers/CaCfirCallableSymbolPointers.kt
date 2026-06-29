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
    /**
     * callable 的稳定缓存键。
     */
    private val cacheKey: CaCfirCallableSymbolCacheKey,
    /**
     * 恢复后需要满足的公开 callable 符号类型。
     */
    private val symbolType: Class<S>,
) : CaCfirSymbolPointerBase<S>() {
    /**
     * 在目标 CFIR session 中按 callableId 和 kind 恢复 callable 符号。
     */
    override fun restoreSymbol(session: org.cangnova.cangjie.analysis.api.CaSession): S? =
        symbolType.castOrNull(
            restoreSession(session)?.restoreCallablePublicSymbol(
                callableId = cacheKey.callableId,
                kind = cacheKey.kind,
            )
        )
}

/**
 * extend 成员 callable 符号 pointer。
 */
internal class CaCfirExtendMemberCallableSymbolPointer<S : CaCallableSymbol>(
    /**
     * extend 成员 callable 的稳定缓存键。
     */
    private val cacheKey: CaCfirExtendMemberCallableSymbolCacheKey,
    /**
     * 恢复后需要满足的公开 callable 符号类型。
     */
    private val symbolType: Class<S>,
) : CaCfirSymbolPointerBase<S>() {
    /**
     * 在目标 CFIR session 中按 extend identity、成员名和 kind 恢复 callable 符号。
     */
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
    /**
     * 顶层 callable 的 callableId。
     */
    private val callableId: CallableId,
    originalSymbol: S?,
) : CaCfirCachedSymbolPointer<S>(originalSymbol) {
    /**
     * 先按 callableId 收集候选，再交给具体 pointer 选择唯一目标。
     */
    final override fun restoreIfNotCached(analysisSession: CaSession): S? {
        require(analysisSession is CaCfirSession)
        val candidates = analysisSession.getCallableSymbols(callableId)
        if (candidates.isEmpty()) return null
        val session = candidates.first().cfir.moduleData.session
        return analysisSession.chooseCandidateAndCreateSymbol(candidates, session)
    }


    /**
     * 从同 callableId 候选中选择匹配目标并构造公开符号。
     */
    protected abstract fun CaCfirSession.chooseCandidateAndCreateSymbol(
        candidates: Collection<CfirCallableSymbol<*>>,
        cfirSession: CfirSession
    ): S?
    /**
     * 判断另一个顶层 callable pointer 是否指向同一个 callable owner。
     */
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
    /**
     * 区分函数重载的 CFIR callable 签名。
     */
    private val signature: CfirCallableSignature,
    originalSymbol: CaNamedFunctionSymbol?,
) : CaTopLevelCallableSymbolPointer<CaNamedFunctionSymbol>(callableId, originalSymbol) {
    /**
     * 在顶层函数候选中按签名选择唯一函数并构造公开符号。
     */
    override fun CaCfirSession.chooseCandidateAndCreateSymbol(
        candidates: Collection<CfirCallableSymbol<*>>,
        cfirSession: CfirSession
    ): CaNamedFunctionSymbol? {
        val function = candidates.findDeclarationWithSignatureBySymbols<CfirNamedFunction>(signature) ?: return null
        return cfirSymbolBuilder.functionBuilder.buildNamedFunctionSymbol(function.symbol)

    }

    /**
     * 判断另一个 pointer 是否指向同一个顶层函数符号。
     */
    fun pointsToTheSameSymbolAs(other: CaSymbolPointer<CaSymbol>): Boolean = this === other ||
        other is CaCfirTopLevelFunctionSymbolPointer &&
        other.signature == signature &&
        hasTheSameOwner(other)
}

@OptIn(CaImplementationDetail::class)
/**
 * 在 CFIR callable 候选集合中查找签名匹配的指定声明类型。
 */
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

/**
 * 按 callableId 从当前 CFIR session 的符号 provider 取得顶层 callable 候选。
 */
internal fun CaCfirSession.getCallableSymbols(callableId: CallableId) =
    cfirSession.symbolProvider.getTopLevelCallableSymbols(callableId.packageName, callableId.callableName)
