package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.diagnostic.ArgumentTypeMismatch
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
import org.cangnova.cangjie.cfir.resolve.calls.candidate.addSubsystemFromAtom
import org.cangnova.cangjie.cfir.resolve.calls.stages.ArgumentCheckingProcessor
import org.cangnova.cangjie.cfir.resolve.calls.stages.CheckerSinkImpl
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeArgumentConstraintPosition
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeVariableForLambdaReturnType
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.asCone
import org.cangnova.cangjie.resolve.calls.inference.isSubtypeConstraintCompatible
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
        if (atom.analyzed) return

        val currentSubstitutor = csImpl.buildCurrentSubstitutor()
        fun substitute(type: ConeCangJieType): ConeCangJieType =
            currentSubstitutor.safeSubstitute(csImpl, type).asCone()

        val parameterTypes = atom.parameterTypes.map(::substitute)
        val expectedReturnType = when {
            csImpl.canBeProper(atom.returnType) -> substitute(atom.returnType)
            csImpl.hasUpperOrEqualUnitConstraint(atom.returnType) -> components.session.builtinTypes.unitType
            else -> null
        }

        val result = lambdaAnalyzer.analyzeAndGetLambdaReturnArguments(
            lambdaAtom = atom,
            parameters = parameterTypes,
            expectedReturnType = expectedReturnType,
            candidate = candidate,
            withPCLASession = withPCLASession,
            forOverloadByLambdaReturnType = false,
        )
        applyResultsOfAnalyzedLambdaToCandidateSystem(csImpl, atom, candidate, result, ::substitute)
    }

    private fun applyResultsOfAnalyzedLambdaToCandidateSystem(
        csImpl: ConstraintSystemImpl,
        atom: ConeResolvedLambdaAtom,
        candidate: Candidate,
        results: ReturnArgumentsAnalysisResult,
        substituteAlreadyFixedVariables: (ConeCangJieType) -> ConeCangJieType,
    ) {
        val (returnAtoms, additionalConstraintStorage) = results
        if (additionalConstraintStorage != null) {
            csImpl.addOtherSystem(additionalConstraintStorage)
        }

        val checkerSink = CheckerSinkImpl(candidate)
        val builder = csImpl.getBuilder()
        val substitutedReturnType = substituteAlreadyFixedVariables(atom.returnType)

        var hasExpressionInReturnArguments = false
        for (returnAtom in returnAtoms) {
            hasExpressionInReturnArguments = true
            csImpl.addSubsystemFromAtom(returnAtom)

            if (!builder.hasContradiction || returnAtom is ConeResolutionAtomWithPostponedChild) {
                ArgumentCheckingProcessor.resolveArgumentExpression(
                    candidate = candidate,
                    atom = returnAtom,
                    expectedType = substitutedReturnType,
                    sink = checkerSink,
                    context = resolutionContext,
                    isReceiver = false,
                    isDispatch = false,
                    anonymousFunctionIfReturnExpression = atom.anonymousFunction,
                )
            }
        }

        if (!hasExpressionInReturnArguments) {
            addLambdaReturnTypeUnitConstraintOrReportError(
                csImpl = csImpl,
                atom = atom,
                checkerSink = checkerSink,
                substituteAlreadyFixedVariables = substituteAlreadyFixedVariables,
            )
        }

        atom.analyzed = true
        atom.returnStatements = returnAtoms
    }

    private fun addLambdaReturnTypeUnitConstraintOrReportError(
        csImpl: ConstraintSystemImpl,
        atom: ConeResolvedLambdaAtom,
        checkerSink: CheckerSinkImpl,
        substituteAlreadyFixedVariables: (ConeCangJieType) -> ConeCangJieType,
    ) {
        val lambdaReturnType = substituteAlreadyFixedVariables(atom.returnType)
        if (lambdaReturnType is ConeErrorType) return

        val builder = csImpl.getBuilder()
        if (builder.hasContradiction) return

        val unitType = components.session.builtinTypes.unitType
        val position = ConeArgumentConstraintPosition(atom.anonymousFunction)

        val compatible = builder.isSubtypeConstraintCompatible(unitType, lambdaReturnType)
        builder.addSubtypeConstraint(unitType, lambdaReturnType, position)
        if (!compatible) {
            val wholeLambdaExpectedType = atom.expectedType?.let(substituteAlreadyFixedVariables) ?: return
            checkerSink.reportDiagnostic(
                ArgumentTypeMismatch(
                    expectedType = wholeLambdaExpectedType,
                    actualType = unitType,
                    argument = atom.expression,
                    isMismatchDueToNullability = false,
                    anonymousFunctionIfReturnExpression = null,
                    systemHadContradiction = builder.hasContradiction,
                )
            )
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
