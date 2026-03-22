package org.cangnova.cangjie.cfir.semantics

import org.cangnova.cangjie.cfir.constraints.CfirConstraintIssue
import org.cangnova.cangjie.cfir.constraints.CfirConstraintSystem
import org.cangnova.cangjie.cfir.constraints.ExplicitReceiverKind
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter

/**
 * Call candidate abstraction.
 */
abstract class AbstractCallCandidate<P : AbstractConeResolutionAtom> : AbstractCandidate() {
    abstract val argumentMapping: LinkedHashMap<P, CfirValueParameter>
    abstract val argumentMappingInitialized: Boolean
    abstract val dispatchReceiver: P?
    abstract val chosenExtensionReceiver: P?
    abstract val explicitReceiverKind: ExplicitReceiverKind
    abstract val contextArguments: List<P>?
    abstract val callInfo: AbstractCallInfo
    abstract val diagnostics: List<ResolutionDiagnostic>
    abstract val errors: List<CfirConstraintIssue>
    abstract val system: CfirConstraintSystem
}
