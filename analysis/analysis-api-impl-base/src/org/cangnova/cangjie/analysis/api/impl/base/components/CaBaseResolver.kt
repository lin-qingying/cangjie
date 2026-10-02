package org.cangnova.cangjie.analysis.api.impl.base.components

import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.components.CaResolver
import org.cangnova.cangjie.analysis.api.impl.base.resolution.CaBaseErrorCallInfo
import org.cangnova.cangjie.analysis.api.impl.base.resolution.CaBaseSuccessCallInfo
import org.cangnova.cangjie.analysis.api.impl.base.resolution.CaBaseSymbolResolutionError
import org.cangnova.cangjie.analysis.api.impl.base.resolution.CaBaseSymbolResolutionSuccess
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.resolution.CaCall
import org.cangnova.cangjie.analysis.api.resolution.CaCallInfo
import org.cangnova.cangjie.analysis.api.resolution.CaCallResolutionAttempt
import org.cangnova.cangjie.analysis.api.resolution.CaCallResolutionError
import org.cangnova.cangjie.analysis.api.resolution.CaCallResolutionSuccess
import org.cangnova.cangjie.analysis.api.resolution.CaCallableMemberCall
import org.cangnova.cangjie.analysis.api.resolution.CaSingleCall
import org.cangnova.cangjie.analysis.api.resolution.CaSymbolResolutionAttempt
import org.cangnova.cangjie.analysis.api.resolution.CaSymbolResolutionSuccess
import org.cangnova.cangjie.analysis.api.resolution.symbols
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjNameReferenceExpression
import org.cangnova.cangjie.psi.CjOperationReferenceExpression

/**
 * 解析协议的基类实现。
 *
 * 对齐 Kotlin `KaBaseResolver` 的分层：这里固定"元素如何分派到后端钩子"，
 * 具体如何从 CFIR 取出符号由具体后端决定。
 */
@CaImplementationDetail
abstract class CaBaseResolver<S : CaSession>(
    final override val analysisSessionProvider: () -> S,
) : CaBaseSessionComponent<S>(), CaResolver {
    /**
     * 由具体前端把调用 PSI 解析为后端解析尝试。
     */
    protected abstract fun CjCallExpression.tryResolveCall(): CaCallResolutionAttempt?

    /**
     * 由具体前端把任意 PSI 元素解析为后端符号解析尝试。
     *
     * 对齐 Kotlin `KaBaseResolver.performSymbolResolution`。
     */
    protected abstract fun performSymbolResolution(element: CjElement): CaSymbolResolutionAttempt?

    /**
     * 将该元素解析为一次完整的调用信息,包含被调用者、实参绑定等;
     * 该元素不构成调用时返回 `null`。
     */
    final override fun CjElement.resolveToCall(): CaCallInfo? = withValidityAssertion {
        val callExpression = this as? CjCallExpression ?: return@withValidityAssertion null
        when (val attempt = callExpression.tryResolveCall()) {
            null -> null
            is CaCallResolutionSuccess -> CaBaseSuccessCallInfo(attempt.call)
            is CaCallResolutionError -> CaBaseErrorCallInfo(attempt.candidateCalls, attempt.diagnostic)
        }
    }

    /**
     * 元素级解析尝试的分派入口。
     *
     * 对齐 Kotlin `KaBaseResolver.tryResolveSymbols`：调用表达式先走调用解析
     * （调用尝试本身已带诊断与候选符号），操作符引用走其所在调用的尝试，
     * 其余元素交给后端钩子。
     */
    final override fun CjElement.tryResolveSymbols(): CaSymbolResolutionAttempt? = withValidityAssertion {
        when (this) {
            is CjCallExpression -> tryResolveSymbolsForResolvableCall()
            is CjOperationReferenceExpression -> tryResolveSymbolsForOperationReference()
            else -> performSymbolResolution(this)
        }
    }

    /**
     * 元素级解析的符号集合；无法解析时返回空集合。
     */
    final override fun CjElement.resolveSymbols(): Collection<CaSymbol> = withValidityAssertion {
        tryResolveSymbols()?.symbols.orEmpty()
    }

    /**
     * 元素级解析的唯一目标符号；不存在唯一结果时返回 `null`。
     */
    final override fun CjElement.resolveSymbol(): CaSymbol? = withValidityAssertion {
        resolveSymbols().singleOrNull()
    }

    /**
     * 调用表达式的符号解析尝试直接由调用解析尝试投影而来：
     * 失败时保留诊断与候选符号，成功时取调用所绑定的符号。
     */
    private fun CjCallExpression.tryResolveSymbolsForResolvableCall(): CaSymbolResolutionAttempt? =
        when (val attempt = tryResolveCall()) {
            is CaCallResolutionError ->
                CaBaseSymbolResolutionError(attempt.diagnostic, attempt.candidateCalls.flatMap { it.symbols })
            is CaCallResolutionSuccess ->
                attempt.call.symbols.toSymbolResolutionSuccessOrNull()
            // 名称引用表达式既可能是调用也可能是类型，退回元素级解析。
            null -> if (this is CjNameReferenceExpression) performSymbolResolution(this) else null
        }

    /**
     * 操作符引用（`+`、`[]` 等）没有独立的调用节点，语义来源是其所在调用。
     */
    private fun CjOperationReferenceExpression.tryResolveSymbolsForOperationReference(): CaSymbolResolutionAttempt? =
        (parent as? CjCallExpression)?.tryResolveSymbolsForResolvableCall()

    /**
     * 非空符号集合投影为成功的元素级解析尝试。
     */
    private fun List<CaSymbol>.toSymbolResolutionSuccessOrNull(): CaSymbolResolutionSuccess? =
        takeIf { it.isNotEmpty() }?.let { symbols ->
            CaBaseSymbolResolutionSuccess(backingSymbols = symbols, token = symbols.first().token)
        }
}

/**
 * [CaCall] 绑定的符号集合。
 *
 * 对齐 Kotlin `KaSingleOrMultiCall.symbols`：单调用取签名符号，成员调用取部分应用符号；
 * 无法定位符号的调用返回空集合。
 */
private val CaCall.symbols: List<CaSymbol>
    get() = when (this) {
        is CaSingleCall<*, *> -> listOf(signature.symbol)
        is CaCallableMemberCall<*, *> -> listOf(partiallyAppliedSymbol.signature.symbol)
        else -> emptyList()
    }