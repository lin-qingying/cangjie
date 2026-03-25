package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.diagnostic.InferenceConstraintError
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CheckerSink
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeTypeProjection


/**
 * Completes the candidate constraint system after fresh variables have been created.
 *
 * Compared with the previous implementation, this stage no longer owns:
 * - creating the constraint system
 * - registering fresh variables
 * - directly applying explicit type arguments as a standalone substitutor shortcut
 *
 * Those responsibilities now live in [CfirCreateFreshTypeVariableSubstitutorStage],
 * matching Kotlin FIR's candidate lifecycle more closely.
 */
object CfirInferTypeArguments :  ResolutionStage() {
    context(sink: CheckerSink, context: ResolutionContext)
    override suspend fun check(candidate: Candidate) {
        candidate.typeArgumentMapping = buildTypeArgumentMapping(candidate)

        if (!candidate.hasReadySubstitutor()) {
            sink.reportDiagnostic(InferenceConstraintError("candidate substitutor is not initialized"))
            return
        }

        if (candidate.system.hasContradiction) {
            sink.reportDiagnostic(InferenceConstraintError("constraint system has contradiction after type argument inference"))
            return
        }

        if (candidate.hasUnresolvedTypeArguments()) {
            sink.reportDiagnostic(InferenceConstraintError("cannot infer all type arguments"))
        }
    }

    private fun buildTypeArgumentMapping(candidate: Candidate): TypeArgumentMapping {
        val explicitTypeArguments = candidate.callInfo.typeArguments
            .mapNotNull { it as? CfirResolvedTypeRef }
            .map { ConeTypeProjection(it.coneType) }

        if (explicitTypeArguments.isEmpty()) {
            return TypeArgumentMapping.NoExplicitArguments
        }

        return TypeArgumentMapping.Mapped(explicitTypeArguments)
    }

    private fun Candidate.hasReadySubstitutor(): Boolean =
        try {
            substitutor
            true
        } catch (_: UninitializedPropertyAccessException) {
            false
        }

    private fun Candidate.hasUnresolvedTypeArguments(): Boolean {
        if (!hasReadySubstitutor()) return true

        return freshVariables.any { freshVariable ->
            val substituted = substitutor.substituteOrSelf(freshVariable.defaultType)
            substituted is ConeErrorType
        }
    }

}
