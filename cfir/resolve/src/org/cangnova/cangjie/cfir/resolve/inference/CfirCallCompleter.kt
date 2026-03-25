package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.CfirResolvable
import org.cangnova.cangjie.cfir.SessionHolder
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.diagnostic.ConeCannotInferValueParameterType
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.lookupTracker
import org.cangnova.cangjie.cfir.resolve.BodyResolveComponents
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.body.CfirAbstractBodyResolveTransformerDispatcher
import org.cangnova.cangjie.cfir.resolve.body.CfirAbstractBodyResolveTransformer
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.ConeAtomWithCandidate
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtom
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolvedLambdaAtom
import org.cangnova.cangjie.cfir.resolve.calls.ConeSimpleLeafResolutionAtom
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirNamedReferenceWithCandidate
import org.cangnova.cangjie.cfir.resolve.calls.stages.TypeArgumentMapping
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeArgumentConstraintPosition
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeExpectedTypeConstraintPosition
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.CfirPCLAInferenceSession
import org.cangnova.cangjie.cfir.resolve.typeFromCallee
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeParameterBasedTypeVariable
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeVariableForLambdaReturnType
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeSimpleCangJieType
import org.cangnova.cangjie.cfir.types.builder.buildErrorTypeRef
import org.cangnova.cangjie.cfir.visitors.transformSingle
import org.cangnova.cangjie.resolve.calls.inference.addEqualityConstraintIfCompatible
import org.cangnova.cangjie.resolve.calls.inference.buildAbstractResultingSubstitutor
import org.cangnova.cangjie.resolve.calls.inference.buildCurrentSubstitutor
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.cangnova.cangjie.resolve.calls.inference.components.PostponedArgumentInputTypesResolver
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.fakeElement
import org.cangnova.cangjie.type.model.safeSubstitute
import org.cangnova.cangjie.types.TypeApproximatorConfiguration
import org.cangnova.cangjie.utils.runIf

/**
 * 调用完成器（Call Completer）。
 *
 * 负责对已解析的调用执行类型推断完成，包括：
 * - 根据期望类型添加约束
 * - 驱动约束系统完成（固定类型变量）
 * - 将最终替换结果写回调用节点
 */
class CfirCallCompleter(
    private val transformer: CfirAbstractBodyResolveTransformerDispatcher,
    private val components: CfirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
) : SessionHolder {
    override val session: CfirSession = components.session
    private val inferenceSession
        get() = transformer.context.inferenceSession

    val completer: ConstraintSystemCompleter = ConstraintSystemCompleter(components)

    /**
     * 对调用执行完整的类型推断完成流程。
     *
     * 流程：
     * 1. 从被调用方提取初始类型
     * 2. 根据期望类型添加约束
     * 3. 计算完成模式（FULL / PARTIAL / PCLA_POSTPONED_CALL）
     * 4. 驱动约束系统完成
     * 5. 将最终替换结果写回调用节点
     */
    fun <T> completeCall(
        call: T,
        resolutionMode: ResolutionMode,
        skipEvenPartialCompletion: Boolean = false,
    ): T where T : CfirResolvable, T : CfirExpression {
        val type = components.typeFromCallee(call)
        val reference = call.calleeReference as? CfirNamedReferenceWithCandidate ?: return call
        val candidate = reference.candidate
        val initialType = type.initialTypeOfCandidate(candidate)



        session.lookupTracker?.recordTypeResolveAsLookup(initialType, call.source, components.context.file.source)

        addConstraintFromExpectedType(candidate, initialType, resolutionMode)

        if (skipEvenPartialCompletion) return call

        val completionMode = candidate.computeCompletionMode(
            session.inferenceComponents, resolutionMode, initialType,
        ).let {
            when {
                it == ConstraintSystemCompletionMode.FULL ->
                    inferenceSession.customCompletionModeInsteadOfFull(call) ?: ConstraintSystemCompletionMode.FULL
                else -> it
            }
        }

        val analyzer = createPostponedArgumentsAnalyzer(transformer.resolutionContext)

        return when (completionMode) {
            ConstraintSystemCompletionMode.FULL -> {
                runCompletionForCall(candidate, completionMode, call, initialType, analyzer)

                val readOnlyConstraintStorage = candidate.system.asReadOnlyStorage()
                checkStorageConstraintsAfterFullCompletion(readOnlyConstraintStorage)

                val finalSubstitutor = readOnlyConstraintStorage
                    .buildAbstractResultingSubstitutor(session.typeContext).asCone()
                call.transformSingle(createCompletionResultsWriter(finalSubstitutor), null)
            }

            ConstraintSystemCompletionMode.PARTIAL, ConstraintSystemCompletionMode.PCLA_POSTPONED_CALL -> {
                runCompletionForCall(candidate, completionMode, call, initialType, analyzer)
                inferenceSession.processPartiallyResolvedCall(call, resolutionMode, completionMode)

                if (candidate.isSyntheticCallForTopLevelLambda()) {
                    // 仅对 PCLA 内的顶层 lambda 合成调用执行预替换，
                    // 将 lambda 的结果类型写入节点以便后续使用，
                    // 但不能完全完成，因为外层类型变量尚未固定。
                    val storage = candidate.system.currentStorage()
                    val finalSubstitutor = storage
                        .buildCurrentSubstitutor(session.typeContext, emptyMap()).asCone()
                    call.transformSingle(createCompletionResultsWriter(finalSubstitutor), null)
                } else {
                    call
                }
            }

            @OptIn(ConstraintSystemCompletionMode.ExclusiveForOverloadResolutionByLambdaReturnType::class)
            ConstraintSystemCompletionMode.UNTIL_FIRST_LAMBDA ->
                throw IllegalStateException()
        }
    }

    private fun Candidate.isSyntheticCallForTopLevelLambda(): Boolean =
        callInfo.callSite is CfirAnonymousFunctionExpression

    /**
     * 验证完全完成后所有类型变量都已被固定。
     *
     * 只检查来自类型参数的类型变量（[ConeTypeParameterBasedTypeVariable]），
     * 其他类型变量（如 lambda 参数类型变量）允许在某些情况下残留。
     */
    private fun checkStorageConstraintsAfterFullCompletion(storage: ConstraintStorage) {
        if (storage.notFixedTypeVariables.isEmpty()) return

        val notFixedTypeVariablesBasedOnTypeParameters = storage.notFixedTypeVariables.filter {
            it.value.typeVariable is ConeTypeParameterBasedTypeVariable
        }

        require(notFixedTypeVariablesBasedOnTypeParameters.isEmpty()) {
            "All variables should be fixed to something, " +
                    "but {${notFixedTypeVariablesBasedOnTypeParameters.keys.joinToString(", ")}} are found"
        }
    }

    /**
     * 根据期望类型向约束系统添加约束。
     *
     * 约束添加策略（按优先级）：
     * 1. cast 位置（`foo() as R`）：添加子类型约束，支持 materialize 风格函数的类型推断
     * 2. 合成函数调用（when/try 等控制流表达式）且强制完全完成：添加等式约束
     * 3. 块最后一条语句且期望类型为 Unit：添加等式约束
     * 4. 一般情况：添加子类型约束作为推断提示
     *
     * 一般情况下使用子类型约束而非等式约束，是为了让类型不匹配的报错在 checker 阶段
     * 产生更清晰的诊断，而不是在约束系统内部产生难以理解的 CS 错误。
     */
    private fun addConstraintFromExpectedType(
        candidate: Candidate,
        initialType: ConeCangJieType,
        resolutionMode: ResolutionMode,
    ) {
        if (resolutionMode !is ResolutionMode.WithExpectedType) return
        val expectedType = resolutionMode.expectedType.fullyExpandedType()

        val system = candidate.system
        when {
            resolutionMode.fromCast -> {
                // cast 位置：仅对 materialize 风格函数（返回单个泛型参数）添加子类型约束
                if (candidate.isFunctionForExpectTypeFromCastFeature()) {
                    system.addSubtypeConstraint(
                        initialType, expectedType,
                        ConeExpectedTypeConstraintPosition,
                    )
                }
            }
            resolutionMode.forceFullCompletion &&
                    candidate.isSyntheticFunctionCallThatShouldUseEqualityConstraint(expectedType) -> {
                // 合成函数调用（when/try 等）：使用等式约束防止分支类型被推断为 Nothing
                system.addEqualityConstraintIfCompatible(
                    initialType, expectedType, ConeExpectedTypeConstraintPosition,
                )
                candidate.markWasExpectedTypeAddedAsEqualityForSyntheticCall()
            }
            expectedType.isUnit && resolutionMode.lastStatementInBlock -> {
                // 块最后一条语句且期望 Unit：使用等式约束
                if (system.notFixedTypeVariables.isEmpty()) return
                system.addEqualityConstraintIfCompatible(
                    initialType, expectedType, ConeExpectedTypeConstraintPosition,
                )
            }
            else -> {
                // 一般情况：子类型约束作为推断提示
                system.addSubtypeConstraint(
                    initialType, expectedType, ConeExpectedTypeConstraintPosition,
                )
            }
        }
    }

    /**
     * 判断合成函数调用是否应使用等式约束。
     *
     * 对 when/try 等控制流表达式建模的合成函数，需要使用等式约束来防止
     * 某个分支类型为 Nothing 时导致整体被错误推断为 Nothing。
     *
     * elvis（`?:`）对应的合成调用排除在外，因为等式约束会影响 elvis RHS 的类型推断。
     * `!!`（CHECK_NOT_NULL）对应的合成调用也排除在外。
     */
    private fun Candidate.isSyntheticFunctionCallThatShouldUseEqualityConstraint(
        expectedType: ConeCangJieType,
    ): Boolean {
        if (components.context.isInsideAssignmentRhs) return false

        val symbol = symbol as? CfirCallableSymbol ?: return false
        if (symbol.origin != CfirDeclarationOrigin.Synthetic.FakeFunction ||
            expectedType.isUnitOrAny() ||
            symbol.callableId == SyntheticCallableId.CHECK_NOT_NULL
        ) {
            return false
        }

        // elvis 对应的合成调用排除：等式约束会影响 RHS 类型推断
        if (system.allTypeVariables.values.any {
                it is ConeTypeParameterBasedTypeVariable &&
                        it.typeParameterSymbol.containingDeclarationSymbol.isSyntheticElvisFunction()
            }
        ) {
            return false
        }

        return true
    }

    /**
     * 判断类型是否为 Any 或 Unit。
     *
     * 用于过滤不需要添加等式约束的期望类型。
     */
    private fun ConeCangJieType.isUnitOrAny(): Boolean {
        val type = unwrap()
        return type.isUnit || type.isAny
    }

    private fun CfirSymbol<*>.isSyntheticElvisFunction(): Boolean {
        return origin == CfirDeclarationOrigin.Synthetic.FakeFunction &&
                (this as? CfirCallableSymbol)?.callableId == SyntheticCallableId.ELVIS
    }

    fun <T> runCompletionForCall(
        candidate: Candidate,
        completionMode: ConstraintSystemCompletionMode,
        call: T,
        initialType: ConeCangJieType,
        analyzer: PostponedArgumentsAnalyzer? = null,
    ) where T : CfirExpression, T : CfirResolvable {
        @Suppress("NAME_SHADOWING")
        val analyzer = analyzer ?: createPostponedArgumentsAnalyzer(transformer.resolutionContext)
        completer.complete(
            candidate.system.asConstraintSystemCompleterContext(),
            completionMode,
            listOf(ConeAtomWithCandidate(call, candidate)),
            initialType,
            transformer.resolutionContext,
        ) { atom, withPCLASession ->
            analyzer.analyze(candidate.system, atom, candidate, withPCLASession)
        }
    }

    /**
     * 为 factory pattern lambda 准备 atom。
     *
     * Factory pattern：通过 lambda 的返回类型反向推断外层泛型参数。
     * 例如：`val x: MyClass = build { MyClass() }`，
     * lambda 返回类型 MyClass 推断出外层 T = MyClass。
     *
     * 为 lambda 的返回类型创建新的类型变量，并将其注入函数类型实参，
     * 再添加子类型约束使新函数类型 <: 原期望函数类型。
     */
    fun prepareLambdaAtomForFactoryPattern(
        atom: ConeResolvedLambdaAtom,
        candidate: Candidate,
    ) {
        val returnVariable = ConeTypeVariableForLambdaReturnType(
            atom.anonymousFunction,
            PostponedArgumentInputTypesResolver.TYPE_VARIABLE_NAME_FOR_LAMBDA_RETURN_TYPE,
        )
        val csBuilder = candidate.system.getBuilder()
        csBuilder.registerVariable(returnVariable)

        val functionalType = csBuilder.buildCurrentSubstitutor()
            .safeSubstitute(csBuilder, atom.expectedType!!) as ConeClassLikeType
        val size = functionalType.typeArguments.size

        // 用新的返回类型变量替换函数类型的最后一个实参（返回类型位置）
        val expectedType = ConeClassLikeTypeImpl(
            functionalType.lookupTag,
            Array(size) { index ->
                if (index != size - 1) functionalType.typeArguments[index]
                else returnVariable.defaultType
            },
            functionalType.attributes,
        )

        csBuilder.addSubtypeConstraint(
            expectedType, functionalType,
            ConeArgumentConstraintPosition(atom.anonymousFunction),
        )
        atom.replaceExpectedType(expectedType, returnVariable.defaultType)
        atom.replaceTypeVariableForLambdaReturnType(returnVariable)
    }

    fun createCompletionResultsWriter(
        substitutor: ConeSubstitutor,
        mode: CfirCallCompletionResultsWriterTransformer.Mode = CfirCallCompletionResultsWriterTransformer.Mode.Normal,
    ): CfirCallCompletionResultsWriterTransformer {
        return CfirCallCompletionResultsWriterTransformer(
            components.session,
            components.scopeSession,
            substitutor,
            components.returnTypeCalculator,
            components.session.typeApproximator,
            components.dataFlowAnalyzer,
            components.integerLiteralAndOperatorApproximationTransformer,
            components.samResolver,
            components.context,
            mode,
        )
    }

    fun completedResultType(candidate: Candidate): ConeCangJieType {
        return candidate.substitutedReturnType()
    }

    fun createPostponedArgumentsAnalyzer(context: ResolutionContext): PostponedArgumentsAnalyzer {
        val lambdaAnalyzer = LambdaAnalyzerImpl()
        return PostponedArgumentsAnalyzer(
            context,
            lambdaAnalyzer,
            session.inferenceComponents,
            transformer.components.callResolver,
        )
    }

    private inner class LambdaAnalyzerImpl : LambdaAnalyzer {
        override fun analyzeAndGetLambdaReturnArguments(
            lambdaAtom: ConeResolvedLambdaAtom,
            parameters: List<ConeCangJieType>,
            expectedReturnType: ConeCangJieType?,
            candidate: Candidate,
            withPCLASession: Boolean,
            forOverloadByLambdaReturnType: Boolean,
        ): ReturnArgumentsAnalysisResult {
            val lambda: CfirAnonymousFunction = lambdaAtom.anonymousFunction

            lambda.matchingParameterFunctionType = lambdaAtom.expectedType

            val expectedReturnTypeRef = expectedReturnType?.let {
                lambda.returnTypeRef.resolvedTypeFromPrototype(
                    it,
                    lambda.source?.fakeElement(CfirFakeSourceElementKind.ImplicitTypeRef),
                )
            }

            // 将推断出的参数类型写入 lambda 的各参数节点
            val lookupTracker = session.lookupTracker
            val fileSource = components.file.source
            lambda.valueParameters.forEachIndexed { index, parameter ->
                if (index >= parameters.size) {
                    // 异常情况：lambda 声明的参数多于期望数量（如错误代码）
                    parameter.replaceReturnTypeRef(
                        buildErrorTypeRef {
                            diagnostic = ConeCannotInferValueParameterType(
                                parameter.symbol,
                                "Lambda or anonymous function has more parameters than expected",
                            )
                            source = parameter.source
                        }
                    )
                    return@forEachIndexed
                }
                val newReturnType = parameters[index].approximateLambdaInputType(
                    parameter.symbol, withPCLASession, candidate,
                )
                val newReturnTypeSource =
                    parameter.source?.fakeElement(CjFakeSourceElementKind.ImplicitReturnTypeOfLambdaValueParameter)
                val newReturnTypeRef = if (parameter.returnTypeRef is CfirImplicitTypeRef) {
                    newReturnType.toCfirResolvedTypeRef(newReturnTypeSource)
                } else {
                    parameter.returnTypeRef.resolvedTypeFromPrototype(newReturnType, newReturnTypeSource)
                }
                parameter.replaceReturnTypeRef(newReturnTypeRef)
                lookupTracker?.recordTypeResolveAsLookup(newReturnTypeRef, parameter.source, fileSource)
            }

            lambda.replaceReturnTypeRef(
                expectedReturnTypeRef?.also {
                    lookupTracker?.recordTypeResolveAsLookup(it, lambda.source, fileSource)
                } ?: components.noExpectedType
            )

            var additionalConstraints: ConstraintStorage? = null

            transformer.context.withAnonymousFunctionTowerDataContext(lambda.symbol) {
                val pclaInferenceSession = runIf(withPCLASession) {
                    candidate.lambdasAnalyzedWithPCLA += lambda
                    CfirPCLAInferenceSession(candidate, session.inferenceComponents)
                }
                val lambdaExpression = lambdaAtom.expression
                val declarationsTransformer = transformer.declarationsTransformer!!

                if (pclaInferenceSession != null) {
                    transformer.context.withInferenceSession(pclaInferenceSession) {
                        declarationsTransformer.doTransformAnonymousFunctionBodyFromCallCompletion(
                            lambdaExpression, expectedReturnTypeRef,
                        )
                        applyResultsToMainCandidate()
                    }
                } else {
                    additionalConstraints =
                        transformer.context.inferenceSession.runLambdaCompletion(
                            candidate, forOverloadByLambdaReturnType,
                        ) {
                            declarationsTransformer.doTransformAnonymousFunctionBodyFromCallCompletion(
                                lambdaExpression, expectedReturnTypeRef,
                            )
                        }
                }
            }
            transformer.context.dropContextForAnonymousFunction(lambda)

            val returnArguments = components.dataFlowAnalyzer
                .returnExpressionsOfAnonymousFunction(lambda)
                .map {
                    val rawAtom = ConeResolutionAtom.createRawAtom(it.expression)
                    when {
                        expectedReturnType == null -> rawAtom
                        rawAtom is ConeAtomWithCandidate -> rawAtom
                        // 带期望类型分析的 lambda，返回语句不应产生新的延迟 atom，
                        // 避免影响外层的变量固定顺序
                        else -> ConeSimpleLeafResolutionAtom(
                            it.expression,
                            allowUnresolvedExpression = false,
                        )
                    }
                }

            return ReturnArgumentsAnalysisResult(returnArguments, additionalConstraints)
        }
    }

    /**
     * 对 lambda 输入类型进行近似，处理类型变量未固定的情况。
     *
     * - PCLA 模式下：来自类型参数的类型变量允许保持未固定（lambda 体内的约束可以反向推断），
     *   其他类型变量（如 lambda 参数类型变量）必须固定，否则报错。
     * - 非 PCLA 模式：所有未固定的类型变量参数类型都报错。
     * - 已固定的类型变量：通过类型近似器近似为超类型后返回。
     */
    private fun ConeCangJieType.approximateLambdaInputType(
        valueParameter: CfirValueParameterSymbol?,
        isRootLambdaForPCLASession: Boolean,
        containingCandidate: Candidate,
    ): ConeCangJieType {
        if (useErrorTypeInsteadOfTypeVariableForParameterType(isRootLambdaForPCLASession)) {
            val diagnostic = valueParameter?.let {
                ConeCannotInferValueParameterType(
                    it,
                    isTopLevelLambda = containingCandidate.isSyntheticCallForTopLevelLambda(),
                )
            } ?: ConeCannotInferValueParameterType(null, "Cannot infer parameter type")
            return ConeErrorType(diagnostic)
        }

        return session.typeApproximator.approximateToSuperType(
            this, TypeApproximatorConfiguration.IntermediateApproximationToSupertypeAfterCompletionInK2,
        ) ?: this
    }

    /**
     * 判断是否应将类型变量替换为错误类型而非保留为类型变量。
     *
     * PCLA 模式下，来自类型参数的类型变量（[originalTypeParameter] != null）
     * 可以保持未固定，由 lambda 体内的约束反向推断。
     * 其他情况下未固定的类型变量参数类型均报错。
     */
    private fun ConeCangJieType.useErrorTypeInsteadOfTypeVariableForParameterType(
        isRootLambdaForPCLASession: Boolean,
    ): Boolean {
        if (this !is ConeTypeVariableType) return false

        if (isRootLambdaForPCLASession || inferenceSession is CfirPCLAInferenceSession) {
            return typeConstructor.originalTypeParameter == null
        }

        return true
    }
}

/**
 * 判断函数是否为 cast 期望类型特性所需的 materialize 风格函数。
 *
 * 条件：
 * - 无显式类型实参
 * - 恰好有一个类型参数
 * - 返回类型即为该类型参数
 * - 值参数和接收者中不包含该类型参数（避免类型信息泄露）
 *
 * 典型形式：`fun <T> materialize(): T`
 */
private fun Candidate.isFunctionForExpectTypeFromCastFeature(): Boolean {
    if (typeArgumentMapping != TypeArgumentMapping.NoExplicitArguments) return false
    val cfir = symbol.cfir as? CfirFunction ?: return false
    return cfir.isFunctionForExpectTypeFromCastFeature()
}

internal fun CfirFunction.isFunctionForExpectTypeFromCastFeature(): Boolean {
    val typeParameter = typeParameters.singleOrNull() ?: return false
    val returnType = returnTypeRef.coneTypeSafe<ConeCangJieType>() ?: return false

    if ((returnType.unwrap() as? ConeTypeParameterType)?.lookupTag !=
        typeParameter.symbol.toLookupTag()
    ) return false

    fun CfirTypeRef.isBadType() =
        coneTypeSafe<ConeCangJieType>()
            ?.contains {
                (it.unwrap() as? ConeTypeParameterType)?.lookupTag == typeParameter.symbol.toLookupTag()
            } != false

    if (valueParameters.any { it.returnTypeRef.isBadType() }) return false

    return true
}

/**
 * 将类型展开为简单类型，去除柔性类型包装。
 * 仓颉没有柔性类型，直接转换为 [ConeSimpleCangJieType]。
 */
private fun ConeCangJieType.unwrap(): ConeSimpleCangJieType = this as ConeSimpleCangJieType
