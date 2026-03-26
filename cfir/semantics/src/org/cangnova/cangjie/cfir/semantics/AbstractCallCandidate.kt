package org.cangnova.cangjie.cfir.semantics

import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.resolve.calls.inference.ConstraintSystem
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemError
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind

abstract class AbstractCallCandidate<P : AbstractConeResolutionAtom> : AbstractCandidate() {
    /**
     * Contains mapping of arguments to value and context parameters (in case of explicit context arguments).
     */
    abstract val argumentMapping: LinkedHashMap<P, CfirValueParameter>
    abstract val argumentMappingInitialized: Boolean
    abstract val dispatchReceiver: AbstractConeResolutionAtom?
    abstract val chosenExtensionReceiver: AbstractConeResolutionAtom?
    abstract val explicitReceiverKind: ExplicitReceiverKind
    abstract val contextArguments: List<AbstractConeResolutionAtom>?
    abstract val callInfo: AbstractCallInfo
    abstract val diagnostics: List<ResolutionDiagnostic>
    abstract val errors: List<ConstraintSystemError>
    abstract val system: ConstraintSystem
    abstract val usedOuterCs: Boolean
}
