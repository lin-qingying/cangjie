package org.cangnova.cangjie.analysis.api.impl.base.components

import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.components.CaResolver
import org.cangnova.cangjie.analysis.api.impl.base.resolution.CaBaseErrorCallInfo
import org.cangnova.cangjie.analysis.api.impl.base.resolution.CaBaseSuccessCallInfo
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.resolution.CaCallInfo
import org.cangnova.cangjie.analysis.api.resolution.CaCallResolutionAttempt
import org.cangnova.cangjie.analysis.api.resolution.CaCallResolutionError
import org.cangnova.cangjie.analysis.api.resolution.CaCallResolutionSuccess
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjElement

/**
 * Analysis API 后端 resolver 的共享调用解析入口。
 *
 * 后端只构造成功或错误的解析尝试；这里统一把尝试投影为稳定的 [CaCallInfo]。
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
     * 将后端解析尝试统一转换为成功或错误的公开调用信息。
     */
    final override fun CjElement.resolveToCall(): CaCallInfo? = withValidityAssertion {
        val callExpression = this as? CjCallExpression ?: return@withValidityAssertion null
        when (val attempt = callExpression.tryResolveCall()) {
            null -> null
            is CaCallResolutionSuccess -> CaBaseSuccessCallInfo(attempt.call)
            is CaCallResolutionError -> CaBaseErrorCallInfo(attempt.candidateCalls, attempt.diagnostic)
        }
    }
}
