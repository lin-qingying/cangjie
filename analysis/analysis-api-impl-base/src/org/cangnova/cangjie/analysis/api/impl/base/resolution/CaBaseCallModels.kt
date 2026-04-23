package org.cangnova.cangjie.analysis.api.impl.base.resolution

import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.diagnostics.CaDiagnostic
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.resolution.CaCall
import org.cangnova.cangjie.analysis.api.resolution.CaCallApplicability
import org.cangnova.cangjie.analysis.api.resolution.CaCallInfo
import org.cangnova.cangjie.analysis.api.resolution.CaCallKind
import org.cangnova.cangjie.analysis.api.resolution.CaCallOrigin
import org.cangnova.cangjie.analysis.api.resolution.CaErrorCallInfo
import org.cangnova.cangjie.analysis.api.resolution.CaSuccessCallInfo
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.name.Name

@CaImplementationDetail
class CaBaseSuccessCallInfo(
    private val backingCall: CaCall,
) : CaSuccessCallInfo {
    override val token: CaLifetimeToken get() = backingCall.token
    override val call: CaCall get() = withValidityAssertion { backingCall }
}


@CaImplementationDetail
class CaBaseErrorCallInfo(
    candidateCalls: List<CaCall>,
    private val backingDiagnostic: CaDiagnostic,
) : CaErrorCallInfo {
    private val backingCandidateCalls: List<CaCall> = candidateCalls
    override val token: CaLifetimeToken get() = backingDiagnostic.token

    override val candidateCalls: List<CaCall> get() = withValidityAssertion { backingCandidateCalls }
    override val diagnostic: CaDiagnostic get() = withValidityAssertion { backingDiagnostic }
}

