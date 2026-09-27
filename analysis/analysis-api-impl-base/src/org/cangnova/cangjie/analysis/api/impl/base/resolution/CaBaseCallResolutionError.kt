package org.cangnova.cangjie.analysis.api.impl.base.resolution

import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.diagnostics.CaDiagnostic
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.resolution.CaCall
import org.cangnova.cangjie.analysis.api.resolution.CaCallResolutionError

/** Analysis API 实现基类中的错误调用解析尝试。 */
@CaImplementationDetail
class CaBaseCallResolutionError(
    candidateCalls: List<CaCall>,
    override val diagnostic: CaDiagnostic,
) : CaCallResolutionError {
    private val backingCandidateCalls: List<CaCall> = candidateCalls.toList()

    override val candidateCalls: List<CaCall>
        get() = withValidityAssertion { backingCandidateCalls }

    override val token: CaLifetimeToken get() = diagnostic.token
}
