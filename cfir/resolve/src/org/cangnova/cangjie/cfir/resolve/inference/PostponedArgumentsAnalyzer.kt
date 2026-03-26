package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.resolve.body.CfirCallResolver
import org.cangnova.cangjie.cfir.resolve.calls.ConeContextSensitiveAlternativeForQualifierAtom
import org.cangnova.cangjie.cfir.resolve.calls.ConeLambdaWithTypeVariableAsExpectedTypeAtom
import org.cangnova.cangjie.cfir.resolve.calls.ConePostponedResolvedAtom
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolvedCallableReferenceAtom
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolvedLambdaAtom
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtomWithPostponedChild
import org.cangnova.cangjie.cfir.resolve.calls.ConeSimpleNameForContextSensitiveResolution
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.stages.ArgumentCheckingProcessor
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeVariableForLambdaReturnType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.asCone
import org.cangnova.cangjie.resolve.calls.inference.ConstraintSystemBuilder
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemImpl
import org.cangnova.cangjie.type.model.safeSubstitute

data class ReturnArgumentsAnalysisResult(
    val returnArguments: Collection<org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtom>,
    val additionalConstraints: ConstraintStorage?,
)

interface LambdaAnalyzer {
    fun analyzeAndGetLambdaReturnArguments(
        lambdaAtom: ConeResolvedLambdaAtom,
        parameters: List<ConeCangJieType>,
        expectedReturnType: ConeCangJieType?,
        candidate: Candidate,
        withPCLASession: Boolean,
        forOverloadByLambdaReturnType: Boolean,
    ): ReturnArgumentsAnalysisResult
}

class PostponedArgumentsAnalyzer(
    private val resolutionContext: ResolutionContext,
    private val lambdaAnalyzer: LambdaAnalyzer,
    private val components: InferenceComponents,
    private val callResolver: CfirCallResolver,
) {
    fun analyze(
        csImpl: ConstraintSystemImpl,
        atom: ConePostponedResolvedAtom,
        candidate: Candidate,
        withPCLASession: Boolean,
    ) {
        when (atom) {
            is ConeResolvedLambdaAtom -> analyzeLambda(csImpl, atom, candidate, withPCLASession)
            is ConeLambdaWithTypeVariableAsExpectedTypeAtom -> {
                val revisedExpectedType = atom.revisedExpectedType?.asCone() ?: atom.expectedType
                if (revisedExpectedType is ConeCangJieType) {
                    val resolved = atom.transformToResolvedLambda(
                        csImpl.getBuilder(),
                        resolutionContext,
                        revisedExpectedType,
                    )
                    analyzeLambda(csImpl, resolved, candidate, withPCLASession)
                } else {
                    atom.analyzed = true
                }
            }

            is ConeResolvedCallableReferenceAtom,
            is ConeSimpleNameForContextSensitiveResolution,
            is ConeContextSensitiveAlternativeForQualifierAtom -> {
                atom.analyzed = true
            }
        }
    }

    private fun analyzeLambda(
        csImpl: ConstraintSystemImpl,
        atom: ConeResolvedLambdaAtom,
        candidate: Candidate,
        withPCLASession: Boolean,
    ) {
        val currentSubstitutor = csImpl.buildCurrentSubstitutor()
        val parameterTypes = atom.parameterTypes.map {
            currentSubstitutor.safeSubstitute(csImpl, it) as ConeCangJieType
        }
        val expectedReturnType = currentSubstitutor.safeSubstitute(csImpl, atom.returnType) as ConeCangJieType

        val result = lambdaAnalyzer.analyzeAndGetLambdaReturnArguments(
            lambdaAtom = atom,
            parameters = parameterTypes,
            expectedReturnType = expectedReturnType,
            candidate = candidate,
            withPCLASession = withPCLASession,
            forOverloadByLambdaReturnType = false,
        )

        atom.analyzed = true
        atom.returnStatements = result.returnArguments

        if (result.additionalConstraints != null) {
            csImpl.addOtherSystem(result.additionalConstraints)
        }
    }
}

fun ConeLambdaWithTypeVariableAsExpectedTypeAtom.transformToResolvedLambda(
    csBuilder: ConstraintSystemBuilder,
    context: ResolutionContext,
    expectedType: ConeCangJieType? = null,
    returnTypeVariable: ConeTypeVariableForLambdaReturnType? = null,
): ConeResolvedLambdaAtom {
    val fixedExpectedType = csBuilder.buildCurrentSubstitutor().asCone()
        .substituteOrSelf(expectedType ?: this.expectedType)
    val resolvedAtom = ArgumentCheckingProcessor.createResolvedLambdaAtomDuringCompletion(
        candidate = candidateOfOuterCall,
        csBuilder = csBuilder,
        atom = ConeResolutionAtomWithPostponedChild(expression),
        expectedType = fixedExpectedType,
        context = context,
        returnTypeVariable = returnTypeVariable,
        anonymousFunctionIfReturnExpression = anonymousFunctionIfReturnExpression,
    )

    subAtom = resolvedAtom
    analyzed = true
    return resolvedAtom
}
