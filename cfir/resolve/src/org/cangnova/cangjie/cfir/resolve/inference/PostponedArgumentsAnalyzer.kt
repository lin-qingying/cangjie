package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.diagnostic.ConeAmbiguityError
import org.cangnova.cangjie.cfir.diagnostic.ConeAmbiguousFunctionReferenceError
import org.cangnova.cangjie.cfir.diagnostic.ConeNoMatchingFunctionReferenceError
import org.cangnova.cangjie.cfir.diagnostic.CallableReferenceFailureKind
import org.cangnova.cangjie.cfir.diagnostic.ArgumentTypeMismatch
import org.cangnova.cangjie.cfir.diagnostic.AmbiguousArgumentType
import org.cangnova.cangjie.cfir.diagnostic.UnsuccessfulCallableReferenceArgument
import org.cangnova.cangjie.cfir.expressions.CfirResolvable
import org.cangnova.cangjie.cfir.expressions.CfirNamedAccessExpression
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind
import org.cangnova.cangjie.cfir.references.CfirErrorNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildErrorNamedReference
import org.cangnova.cangjie.cfir.resolve.body.CallableReferenceResolutionResult
import org.cangnova.cangjie.cfir.resolve.body.CfirCallResolver
import org.cangnova.cangjie.cfir.resolve.calls.ConeContextSensitiveAlternativeForQualifierAtom
import org.cangnova.cangjie.cfir.resolve.calls.ConeLambdaWithTypeVariableAsExpectedTypeAtom
import org.cangnova.cangjie.cfir.resolve.calls.ConePostponedResolvedAtom
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtom
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolvedCallableReferenceAtom
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolvedLambdaAtom
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtomWithPostponedChild
import org.cangnova.cangjie.cfir.resolve.calls.ConeSimpleNameForContextSensitiveResolution
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirNamedReferenceWithCandidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.addSubsystemFromAtom
import org.cangnova.cangjie.cfir.resolve.calls.stages.ArgumentCheckingProcessor
import org.cangnova.cangjie.cfir.resolve.calls.stages.CheckerSinkImpl
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.functionTypeForFunctionValueCandidate
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeArgumentConstraintPosition
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeVariableForLambdaReturnType
import org.cangnova.cangjie.cfir.semantics.ErrorTypeInArguments
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.asCone
import org.cangnova.cangjie.cfir.types.commonSuperTypeOrNull
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.resolve.calls.inference.addSubtypeConstraintIfCompatible
import org.cangnova.cangjie.resolve.calls.inference.isSubtypeConstraintCompatible
import org.cangnova.cangjie.resolve.calls.inference.ConstraintSystemBuilder
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemImpl
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.type.model.safeSubstitute

/**
 * lambda 返回实参分析结果。
 */
data class ReturnArgumentsAnalysisResult(
    /**
     * lambda body 中可作为返回值参与约束的表达式 atom。
     */
    val returnArguments: Collection<org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtom>,
    /**
     * lambda body 分析额外产生的约束存储。
     */
    val additionalConstraints: ConstraintStorage?,
)

/**
 * lambda body 分析器接口。
 */
interface LambdaAnalyzer {
    /**
     * 分析 lambda 并返回其返回表达式 atom 与额外约束。
     */
    fun analyzeAndGetLambdaReturnArguments(
        lambdaAtom: ConeResolvedLambdaAtom,
        parameters: List<ConeCangJieType>,
        expectedReturnType: ConeCangJieType?,
        candidate: Candidate,
        withPCLASession: Boolean,
        forOverloadByLambdaReturnType: Boolean,
    ): ReturnArgumentsAnalysisResult
}

/**
 * 延迟实参分析器。
 *
 * 该组件在调用完成阶段按约束系统当前状态分析 lambda、callable reference 和上下文敏感名称，
 * 并把分析结果回写到顶层候选的约束系统。
 */
class PostponedArgumentsAnalyzer(
    /**
     * 当前调用解析上下文。
     */
    private val resolutionContext: ResolutionContext,
    /**
     * lambda body 分析器。
     */
    private val lambdaAnalyzer: LambdaAnalyzer,
    /**
     * 当前会话的推断组件集合。
     */
    private val components: InferenceComponents,
    /**
     * 用于上下文敏感函数引用二次解析的调用解析器。
     */
    private val callResolver: CfirCallResolver,
) {
    /**
     * 按延迟 atom 类型执行对应分析。
     */
    fun analyze(
        csImpl: ConstraintSystemImpl,
        atom: ConePostponedResolvedAtom,
        candidate: Candidate,
        withPCLASession: Boolean,
    ) {
        when (atom) {
            is ConeResolvedLambdaAtom -> analyzeLambda(
                csImpl = csImpl,
                atom = atom,
                candidate = candidate,
                withPCLASession = withPCLASession,
                forOverloadByLambdaReturnType = false,
            )
            is ConeLambdaWithTypeVariableAsExpectedTypeAtom -> {
                val revisedExpectedType = atom.revisedExpectedType?.asCone() ?: atom.expectedType
                if (revisedExpectedType is ConeCangJieType) {
                    val resolved = atom.transformToResolvedLambda(
                        csImpl.getBuilder(),
                        resolutionContext,
                        revisedExpectedType,
                    )
                    analyzeLambda(
                        csImpl = csImpl,
                        atom = resolved,
                        candidate = candidate,
                        withPCLASession = withPCLASession,
                        forOverloadByLambdaReturnType = false,
                    )
                } else {
                    atom.analyzed = true
                }
            }

            is ConeResolvedCallableReferenceAtom -> processCallableReference(atom, candidate)

            is ConeSimpleNameForContextSensitiveResolution -> {
                processFunctionReferenceArgument(atom, candidate)
            }

            is ConeContextSensitiveAlternativeForQualifierAtom -> {
                atom.analyzed = true
            }
        }
    }

    /**
     * 在 completion 阶段重新解析首轮因重载歧义推迟的 callable reference。
     */
    private fun processCallableReference(
        atom: ConeResolvedCallableReferenceAtom,
        candidate: Candidate,
    ) {
        if (!atom.needsResolution) {
            atom.markResolved()
            return
        }

        when (callResolver.resolveCallableReferenceArguments(candidate, listOf(atom))) {
            CallableReferenceResolutionResult.RESOLVED -> return
            CallableReferenceResolutionResult.POSTPONED -> return
            CallableReferenceResolutionResult.FAILURE -> {
                when (atom.failureKind) {
                    CallableReferenceFailureKind.AMBIGUOUS_ARGUMENT_TYPE -> candidate.addDiagnostic(
                        AmbiguousArgumentType(candidate.callInfo.callSite, atom.expression),
                    )

                    CallableReferenceFailureKind.GENERIC_TYPE_ARGUMENT_REQUIRED -> candidate.addDiagnostic(
                        UnsuccessfulCallableReferenceArgument(
                            atom.expression,
                            CallableReferenceFailureKind.GENERIC_TYPE_ARGUMENT_REQUIRED,
                        )
                    )

                    else -> candidate.addDiagnostic(
                        UnsuccessfulCallableReferenceArgument(
                            atom.expression,
                            atom.failureKind ?: CallableReferenceFailureKind.NO_MATCH,
                        )
                    )
                }
                atom.markResolved()
            }
        }
    }

    /**
     * 处理上下文敏感函数引用实参。
     */
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
            .mapNotNull { candidate -> candidate as? Candidate }
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

        val matchingCandidates = matchingFunctionReferenceCandidatesByExpectedType(
            atom,
            functionCandidates,
            topLevelCandidate,
        )
        if (matchingCandidates == null) {
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

        if (matchingCandidates.size == 1) {
            val selectedCandidate = matchingCandidates.single()
            val functionType = resolutionContext.bodyResolveComponents
                .functionTypeForFunctionValueCandidate(selectedCandidate)
            expression.replaceCalleeReference(
                CfirNamedReferenceWithCandidate(
                    errorReference.source,
                    errorReference.name,
                    selectedCandidate,
                )
            )
            expression.replaceConeTypeOrNull(functionType)
            ArgumentCheckingProcessor.resolveArgumentExpression(
                topLevelCandidate,
                ConeResolutionAtom.createRawAtom(expression),
                atom.expectedType,
                CheckerSinkImpl(topLevelCandidate),
                context = resolutionContext,
                isReceiver = false,
                isDispatch = false,
            )
            return
        }

        val hasExplicitTypeArguments = expression.typeArguments.isNotEmpty() || functionCandidates.any { candidate ->
            candidate.callInfo.hasExplicitTypeArguments
        }
        val hasIgnoredGenericCandidate = !hasExplicitTypeArguments && functionCandidates.any { candidate ->
            val function = candidate.symbol.takeIf { it.isBound }?.cfir as? CfirFunction
            function?.typeParameters?.isNotEmpty() == true
        }

        val diagnostic = when {
            matchingCandidates.size > 1 -> {
                topLevelCandidate.addDiagnostic(
                    UnsuccessfulCallableReferenceArgument(expression, CallableReferenceFailureKind.AMBIGUITY)
                )
                ConeAmbiguousFunctionReferenceError(
                    name = errorReference.name,
                    candidatesWithErrors = matchingCandidates.associateWith { null },
                )
            }

            hasIgnoredGenericCandidate -> {
                topLevelCandidate.addDiagnostic(
                    UnsuccessfulCallableReferenceArgument(
                        expression,
                        CallableReferenceFailureKind.GENERIC_TYPE_ARGUMENT_REQUIRED,
                    )
                )
                ConeSimpleDiagnostic(
                    "generic function reference should be used with type argument",
                    DiagnosticKind.GenericTypeWithoutTypeArgument,
                )
            }

            else -> {
                topLevelCandidate.addDiagnostic(UnsuccessfulCallableReferenceArgument(expression))
                ConeNoMatchingFunctionReferenceError(errorReference.name)
            }
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

    /**
     * 使用外层候选当前约束系统替换目标函数类型，再收集所有满足目标类型的函数引用候选。
     */
    private fun matchingFunctionReferenceCandidatesByExpectedType(
        atom: ConeSimpleNameForContextSensitiveResolution,
        candidates: List<Candidate>,
        topLevelCandidate: Candidate,
    ): Set<Candidate>? {
        val systemBuilder = topLevelCandidate.system.getBuilder()
        val expectedFunctionType = systemBuilder.buildCurrentSubstitutor().asCone()
            .safeSubstitute(systemBuilder, atom.expectedType)
            .asCone()
            .fullyExpandedType(resolutionContext.session) as? ConeFunctionType ?: return null
        return candidates.filterTo(linkedSetOf()) { candidate ->
            val functionType = resolutionContext.bodyResolveComponents
                .functionTypeForFunctionValueCandidate(candidate)
            val matches = AbstractTypeChecker.isSubtypeOfForFunctionReference(
                resolutionContext.typeContext,
                functionType,
                expectedFunctionType,
            )
            matches
        }
    }

    /**
     * 按当前候选约束系统分析 lambda，并把 lambda body 产生的返回约束写回候选系统。
     *
     * 该方法对应 Kotlin FIR `PostponedArgumentsAnalyzer.analyzeLambda`：普通 call
     * completion 和 overload-by-lambda 分支必须复用同一条 postponed-argument 分析路径，
     * 避免在重载解析层重新发明 lambda body resolve 流程。
     */
    internal fun analyzeLambda(
        csImpl: ConstraintSystemImpl,
        atom: ConeResolvedLambdaAtom,
        candidate: Candidate,
        withPCLASession: Boolean,
        forOverloadByLambdaReturnType: Boolean,
    ): ReturnArgumentsAnalysisResult {
        if (atom.analyzed) {
            return ReturnArgumentsAnalysisResult(atom.returnStatements, additionalConstraints = null)
        }

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
            forOverloadByLambdaReturnType = forOverloadByLambdaReturnType,
        )
        applyResultsOfAnalyzedLambdaToCandidateSystem(csImpl, atom, candidate, result, ::substitute)
        return result
    }

    /**
     * 将已经完成 body resolve 的 lambda 返回 atoms 应用到指定候选系统。
     *
     * 对齐 Kotlin FIR 同名职责：overload-by-lambda 分支可以先分析一个 lambda body，
     * 再把返回表达式约束映射到候选系统，而不是重新跑完整调用完成流程。
     */
    internal fun applyResultsOfAnalyzedLambdaToCandidateSystem(
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

        checkLambdaValueCompatibilityAfterBody(
            csImpl = csImpl,
            atom = atom,
            candidate = candidate,
            checkerSink = checkerSink,
            returnAtoms = returnAtoms,
            hasExpressionInReturnArguments = hasExpressionInReturnArguments,
        )

        atom.analyzed = true
        atom.returnStatements = returnAtoms
    }

    /**
     * lambda 没有函数型 expected type 时，在 body 分析后按自身参数和真实返回值合成函数类型。
     *
     * 该时点晚于返回表达式解析、早于候选完成写回，既能避免 fresh return variable 掩盖
     * 根 classifier 不兼容，也能把失败保留为普通 [ArgumentTypeMismatch]，供候选选择和
     * 错误引用写回共同消费。
     */
    private fun checkLambdaValueCompatibilityAfterBody(
        csImpl: ConstraintSystemImpl,
        atom: ConeResolvedLambdaAtom,
        candidate: Candidate,
        checkerSink: CheckerSinkImpl,
        returnAtoms: Collection<ConeResolutionAtom>,
        hasExpressionInReturnArguments: Boolean,
    ) {
        val declaredExpectedType = atom.expectedType ?: return
        val builder = csImpl.getBuilder()
        if (builder.hasContradiction) return
        if (candidate.diagnostics.any { diagnostic ->
            diagnostic is ArgumentTypeMismatch && diagnostic.argument === atom.expression
        }) return

        val currentSubstitutor = csImpl.buildCurrentSubstitutor()
        fun substitute(type: ConeCangJieType): ConeCangJieType =
            currentSubstitutor.safeSubstitute(csImpl, type).asCone()

        val expectedType = substitute(declaredExpectedType)
        if (expectedType is ConeErrorType) return
        if (expectedType.fullyExpandedType(components.session) is ConeFunctionType) return

        val expressionReturnTypes = returnAtoms.asSequence()
            .filterNot { returnAtom -> returnAtom.expression.isImplicitUnitForEmptyLambda() }
            .mapNotNull { returnAtom -> returnAtom.expression.coneTypeOrNull }
            .map(::substitute)
            .toList()
        val actualReturnType = when {
            !hasExpressionInReturnArguments -> components.session.builtinTypes.unitType
            expressionReturnTypes.isNotEmpty() ->
                components.session.typeContext.commonSuperTypeOrNull(expressionReturnTypes)
                    ?: substitute(atom.returnType)
            else -> substitute(atom.returnType)
        }
        val actualLambdaType = ConeFunctionType(
            parameterTypes = atom.parameterTypes.map(::substitute),
            returnType = actualReturnType,
        )
        val position = ConeArgumentConstraintPosition(atom.expression)
        if (builder.addSubtypeConstraintIfCompatible(actualLambdaType, expectedType, position)) return

        checkerSink.reportDiagnostic(
            ArgumentTypeMismatch(
                expectedType = expectedType,
                actualType = actualLambdaType,
                argument = atom.expression,
                isMismatchDueToNullability = false,
                anonymousFunctionIfReturnExpression = null,
                systemHadContradiction = false,
            )
        )
    }

    /**
     * 对没有真实返回表达式的 lambda 添加 Unit 返回约束，或报告返回类型不匹配。
     */
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

/**
 * 判断表达式是否是空 lambda 的隐式 Unit。
 */
private fun org.cangnova.cangjie.cfir.expressions.CfirExpression.isImplicitUnitForEmptyLambda(): Boolean {
    return source?.kind == CjFakeSourceElementKind.ImplicitUnit.ForEmptyLambda
}

/**
 * 判断表达式是否已经携带解析错误。
 */
private fun org.cangnova.cangjie.cfir.expressions.CfirExpression.hasResolutionError(): Boolean {
    if (coneTypeOrNull is ConeErrorType) return true
    return this is CfirResolvable && calleeReference is CfirErrorNamedReference
}

/**
 * 将“期望类型为类型变量”的 lambda atom 转换为已解析 lambda atom。
 */
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
