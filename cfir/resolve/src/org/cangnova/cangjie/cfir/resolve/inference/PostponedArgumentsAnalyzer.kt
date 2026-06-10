package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.diagnostic.AmbiguousArgumentType
import org.cangnova.cangjie.cfir.diagnostic.ConeAmbiguityError
import org.cangnova.cangjie.cfir.diagnostic.ArgumentTypeMismatch
import org.cangnova.cangjie.cfir.expressions.CfirResolvable
import org.cangnova.cangjie.cfir.expressions.CfirNamedAccessExpression
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind
import org.cangnova.cangjie.cfir.references.CfirErrorNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildErrorNamedReference
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
import org.cangnova.cangjie.cfir.semantics.ErrorTypeInArguments
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeUnreportedDuplicateDiagnostic
import org.cangnova.cangjie.cfir.types.asCone
import org.cangnova.cangjie.resolve.calls.inference.isSubtypeConstraintCompatible
import org.cangnova.cangjie.resolve.calls.inference.ConstraintSystemBuilder
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemImpl
import org.cangnova.cangjie.source.CjFakeSourceElementKind
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

            is ConeResolvedCallableReferenceAtom -> {
                atom.analyzed = true
            }

            is ConeSimpleNameForContextSensitiveResolution -> {
                processFunctionReferenceArgument(atom, candidate)
            }

            is ConeContextSensitiveAlternativeForQualifierAtom -> {
                atom.analyzed = true
            }
        }
    }

    private fun processFunctionReferenceArgument(
        atom: ConeSimpleNameForContextSensitiveResolution,
        topLevelCandidate: Candidate,
    ) {
        if (atom.analyzed) return
        atom.analyzed = true

        val expression = atom.expression as? CfirNamedAccessExpression ?: return
        val errorReference = expression.calleeReference as? CfirErrorNamedReference ?: return
        val ambiguity = errorReference.diagnostic as? ConeAmbiguityError ?: return
        val functionCandidates = ambiguity.candidates
            .filter { candidate -> candidate.symbol.takeIf { it.isBound }?.cfir is CfirFunction }
        if (functionCandidates.size != ambiguity.candidates.size) {
            ArgumentCheckingProcessor.resolveArgumentExpression(
                topLevelCandidate,
                atom.fallbackSubAtom,
                atom.expectedType,
                CheckerSinkImpl(topLevelCandidate),
                context = resolutionContext,
                isReceiver = false,
                isDispatch = false,
            )
            return
        }

        val hasExplicitTypeArguments = expression.typeArguments.isNotEmpty() ||
            functionCandidates
                .filterIsInstance<org.cangnova.cangjie.cfir.semantics.AbstractCallCandidate<*>>()
                .any { candidate -> candidate.callInfo.hasExplicitTypeArguments }

        val diagnostic = if (hasExplicitTypeArguments) {
            topLevelCandidate.addDiagnostic(AmbiguousArgumentType(topLevelCandidate.callInfo.explicitReceiver ?: topLevelCandidate.callInfo.callSite, expression))
            ConeUnreportedDuplicateDiagnostic(ambiguity)
        } else {
            topLevelCandidate.addDiagnostic(ErrorTypeInArguments)
            ConeSimpleDiagnostic(
                "generic function reference should be used with type argument",
                DiagnosticKind.GenericTypeWithoutTypeArgument,
            )
        }

        expression.replaceCalleeReference(
            buildErrorNamedReference {
                source = errorReference.source
                name = errorReference.name
                this.diagnostic = diagnostic
            }
        )
        expression.replaceConeTypeOrNull(ConeErrorType(diagnostic, delegatedType = atom.expectedType))
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
        val lastExpression = atom.anonymousFunction.body
            ?.statements
            ?.lastOrNull() as? org.cangnova.cangjie.cfir.expressions.CfirExpression
        val isLastExpressionCoercedToUnit = substitutedReturnType.isUnit

        var hasExpressionInReturnArguments = false
        for (returnAtom in returnAtoms) {
            val expression = returnAtom.expression
            if (expression.isImplicitUnitForEmptyLambda()) continue

            val haveSubsystem = csImpl.addSubsystemFromAtom(returnAtom)
            val isLastExpression = expression === lastExpression

            /**
             * 参考 Kotlin FIR 的 postponed lambda 分析：
             *
             * 当 lambda 的返回类型已经被约束为 `Unit` 时，最后一个表达式只承担“语句求值”
             * 语义，不应该再被当作真实返回值去反向约束外层调用。
             *
             * 否则像 `break/continue/throw` 这类局部控制流错误会经由返回值检查泄漏到
             * 外层高阶函数调用，把原本可解析的调用错误地打成 `ErrorTypeInArguments`
             * 甚至 `UNRESOLVED_REFERENCE`。
             */
            if (isLastExpression && isLastExpressionCoercedToUnit) {
                val expressionType = expression.coneTypeOrNull
                if (haveSubsystem && expressionType != null) {
                    val compatible = builder.isSubtypeConstraintCompatible(expressionType, substitutedReturnType)
                    if (compatible) {
                        builder.addSubtypeConstraint(
                            expressionType,
                            substitutedReturnType,
                            ConeArgumentConstraintPosition(expression),
                        )
                    }
                }
                continue
            }

            hasExpressionInReturnArguments = true
            if (expression.hasResolutionError()) {
                checkerSink.reportDiagnostic(ErrorTypeInArguments)
                continue
            }

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
            } else if (expression.hasResolutionError()) {
                checkerSink.reportDiagnostic(ErrorTypeInArguments)
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

private fun org.cangnova.cangjie.cfir.expressions.CfirExpression.isImplicitUnitForEmptyLambda(): Boolean {
    return source?.kind == CjFakeSourceElementKind.ImplicitUnit.ForEmptyLambda
}

private fun org.cangnova.cangjie.cfir.expressions.CfirExpression.hasResolutionError(): Boolean {
    if (coneTypeOrNull is ConeErrorType) return true
    return this is CfirResolvable && calleeReference is CfirErrorNamedReference
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
