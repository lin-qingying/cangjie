package org.cangnova.cangjie.cfir.resolve.calls.candidate

import org.cangnova.cangjie.cfir.diagnostic.HiddenCandidate
import org.cangnova.cangjie.cfir.diagnostic.InferenceConstraintError
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.SpecialNames
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind

/**
 * Small construction seam for candidates discovered during tower traversal.
 *
 * This keeps tower traversal focused on scope walking while preserving the current
 * receiver/base-system defaults used by local call resolution.
 */
class CandidateFactory(
    private val context: ResolutionContext,
    private val baseSystem: ConstraintStorage = ConstraintStorage.Empty,
) {
    fun createCandidate(
        callInfo: CallInfo,
        symbol: CfirCallableSymbol<*>,
        originScope: CfirScope?,
        explicitReceiverKind: ExplicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
    ): Candidate {
        return Candidate(
            symbol = symbol,
            dispatchReceiver = null,
            givenExtensionReceiver = null,
            explicitReceiverKind = explicitReceiverKind,
            constraintSystemFactory = context.inferenceComponents.constraintSystemFactory,
            baseSystem = baseSystem,
            callInfo = callInfo,
            originScope = originScope,
            bodyResolveContext = context.bodyResolveContext,
        )
    }

    fun createErrorCandidate(callInfo: CallInfo, diagnostic: ConeDiagnostic): Candidate {
        val candidate = createCandidate(
            callInfo = callInfo,
            symbol = CfirNamedFunctionSymbol(CallableId(SpecialNames.NO_NAME_PROVIDED)),
            originScope = null,
        )
        when (diagnostic) {
            is org.cangnova.cangjie.cfir.diagnostic.ConeHiddenCandidateError ->
                candidate.addDiagnostic(HiddenCandidate())
            else ->
                candidate.addDiagnostic(InferenceConstraintError(diagnostic.reason))
        }
        return candidate
    }
}
