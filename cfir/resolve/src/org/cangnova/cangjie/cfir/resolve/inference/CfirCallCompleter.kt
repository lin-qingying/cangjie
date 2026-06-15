package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirLookupTrackerComponent
import org.cangnova.cangjie.cfir.SessionHolder
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.diagnostic.ConeCannotInferValueParameterType
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirResolvable
import org.cangnova.cangjie.cfir.lookupTracker
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.body.CfirAbstractBodyResolveTransformer
import org.cangnova.cangjie.cfir.resolve.body.CfirAbstractBodyResolveTransformerDispatcher
import org.cangnova.cangjie.cfir.resolve.calls.ConeAtomWithCandidate
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtom
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolvedLambdaAtom
import org.cangnova.cangjie.cfir.resolve.calls.ConeSimpleLeafResolutionAtom
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirNamedReferenceWithCandidate
import org.cangnova.cangjie.cfir.resolve.calls.stages.TypeArgumentMapping
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeArgumentConstraintPosition
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeExpectedTypeConstraintPosition
import org.cangnova.cangjie.cfir.resolve.initialTypeOfCandidate
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.CfirPCLAInferenceSession
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.resultType
import org.cangnova.cangjie.cfir.resolve.typeFromCallee
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeVariableForLambdaReturnType
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeSubstitutorByMap
import org.cangnova.cangjie.cfir.types.ConeAnyType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConeSimpleCangJieType
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.types.arrayLiteralElementType
import org.cangnova.cangjie.cfir.types.asCone
import org.cangnova.cangjie.cfir.types.builder.buildErrorTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.cfir.types.classId
import org.cangnova.cangjie.cfir.types.coneTypeSafe
import org.cangnova.cangjie.cfir.types.contains
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.cfir.types.typeApproximator
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.cfir.visitors.transformSingle
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.resolve.calls.inference.addEqualityConstraintIfCompatible
import org.cangnova.cangjie.resolve.calls.inference.buildAbstractResultingSubstitutor
import org.cangnova.cangjie.resolve.calls.inference.buildCurrentSubstitutor
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.cangnova.cangjie.resolve.calls.inference.components.PostponedArgumentInputTypesResolver
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.fakeElement
import org.cangnova.cangjie.types.TypeApproximatorConfiguration
import org.cangnova.cangjie.utils.runIf

class CfirCallCompleter(
    private val transformer: CfirAbstractBodyResolveTransformerDispatcher,
    private val components: CfirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
) : SessionHolder {
    override val session: CfirSession = components.session

    private val inferenceSession
        get() = transformer.context.inferenceSession

    val completer: ConstraintSystemCompleter = ConstraintSystemCompleter(components)

    fun <T> completeCall(
        call: T,
        resolutionMode: ResolutionMode,
        skipEvenPartialCompletion: Boolean = false,
    ): T where T : CfirResolvable, T : CfirExpression {
        val reference = call.calleeReference as? CfirNamedReferenceWithCandidate ?: return call
        val candidate = reference.candidate
        val initialType = components.typeFromCallee(call).initialTypeOfCandidate(candidate)

        // Annotation types are resolved during type resolution, and generic arguments aren't inferred.
        // Updating the type of an annotation call is a no-op, it only checks if it's the same as the type of the annotation type ref.
        // In the case of a generic annotation, we would set it to a type containing type variable types which would cause an exception.
        // Delegated constructor calls always have type Unit but typeFromCallee returns the type of the superclass.
        if (call !is CfirAnnotationCall && call !is CfirAnonymousFunctionExpression) {
            call.resultType = initialType
        }

        session.lookupTracker?.recordTypeResolveAsLookup(initialType, call.source, components.context.file.source)
        candidate.noArgEnumConstructorTargetTypeSubstitutor(initialType, resolutionMode)?.let { substitutor ->
            return call.transformSingle(createCompletionResultsWriter(substitutor), null)
        }
        addConstraintFromExpectedType(candidate, initialType, resolutionMode)

        if (skipEvenPartialCompletion) return call

        val completionMode = candidate.computeCompletionMode(
            session.inferenceComponents,
            resolutionMode,
            initialType,
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
                val finalSubstitutor = candidate.system.asReadOnlyStorage()
                    .buildAbstractResultingSubstitutor(session.typeContext)
                    .asCone()
                call.transformSingle(createCompletionResultsWriter(finalSubstitutor), null)
            }

            ConstraintSystemCompletionMode.PARTIAL,
            ConstraintSystemCompletionMode.PCLA_POSTPONED_CALL -> {
                runCompletionForCall(candidate, completionMode, call, initialType, analyzer)
                inferenceSession.processPartiallyResolvedCall(call, resolutionMode, completionMode)
                if (candidate.isSyntheticCallForTopLevelLambda()) {
                    val finalSubstitutor = candidate.system.currentStorage()
                        .buildCurrentSubstitutor(session.typeContext, emptyMap())
                        .asCone()
                    call.transformSingle(createCompletionResultsWriter(finalSubstitutor), null)
                } else {
                    call
                }
            }

            @OptIn(ConstraintSystemCompletionMode.ExclusiveForOverloadResolutionByLambdaReturnType::class)
            ConstraintSystemCompletionMode.UNTIL_FIRST_LAMBDA -> error("Unexpected completion mode")
        }
    }

    private fun Candidate.isSyntheticCallForTopLevelLambda(): Boolean =
        callInfo.callSite is CfirAnonymousFunctionExpression

    private fun addConstraintFromExpectedType(
        candidate: Candidate,
        initialType: ConeCangJieType,
        resolutionMode: ResolutionMode,
    ) {
        if (resolutionMode !is ResolutionMode.WithExpectedType) return
        val expectedType = resolutionMode.expectedType.fullyExpandedType()
        if (!candidate.shouldUseExpectedTypeForCompletion(initialType, expectedType)) return
        val system = candidate.system

        if (candidate.addBuiltinArrayConstructorExpectedElementConstraint(expectedType)) return
        if (candidate.addEnumConstructorExpectedTypeConstraint(initialType, expectedType)) return

        when {
            resolutionMode.fromCast -> {
                if (candidate.isFunctionForExpectTypeFromCastFeature()) {
                    system.addSubtypeConstraint(initialType, expectedType, ConeExpectedTypeConstraintPosition)
                }
            }

            resolutionMode.forceFullCompletion &&
                    candidate.isSyntheticFunctionCallThatShouldUseEqualityConstraint(expectedType) -> {
                system.addEqualityConstraintIfCompatible(initialType, expectedType, ConeExpectedTypeConstraintPosition)
                candidate.markWasExpectedTypeAddedAsEqualityForSyntheticCall()
            }

            with(session.typeContext) { expectedType.isUnit() } && resolutionMode.lastStatementInBlock -> {
                if (system.notFixedTypeVariables.isEmpty()) return
                system.addEqualityConstraintIfCompatible(initialType, expectedType, ConeExpectedTypeConstraintPosition)
            }

            else -> {
                system.addSubtypeConstraint(initialType, expectedType, ConeExpectedTypeConstraintPosition)
            }
        }
    }

    /**
     * 官方内建数组构造在无显式类型实参时会用左侧目标数组的元素类型约束构造器泛型 `T`。
     *
     * 显式 `Array<T>(...)` / `VArray<T, $N>(...)` 已经由类型实参固定元素类型；此时左侧期望类型属于
     * 初始化表达式整体检查，不能再反向制造调用推断错误。
     */
    private fun Candidate.addBuiltinArrayConstructorExpectedElementConstraint(
        expectedType: ConeCangJieType,
    ): Boolean {
        val callable = symbol.takeIf { it.isBound }?.cfir as? CfirFunction ?: return false
        if (callable.origin != CfirDeclarationOrigin.Synthetic.BuiltinArrayConstructor) return false
        if (callInfo.hasExplicitTypeArguments) return false

        val expectedElementType = expectedType.fullyExpandedType().arrayLiteralElementType ?: return false
        val elementVariableType = freshVariables.singleOrNull()?.defaultType as? ConeCangJieType ?: return false
        system.addSubtypeConstraint(elementVariableType, expectedElementType, ConeExpectedTypeConstraintPosition)
        return true
    }

    /**
     * 官方 enum sugar 在目标类型能确定同一个 enum owner 时，直接把该目标类型
     * 作为 enum constructor 表达式类型；这比普通 subtype 约束更强，能够保留
     * `Option<T>` 这类仍含声明类型参数的上下文。
     */
    private fun Candidate.addEnumConstructorExpectedTypeConstraint(
        initialType: ConeCangJieType,
        expectedType: ConeCangJieType,
    ): Boolean {
        if (symbol.takeIf { it.isBound }?.cfir !is CfirEnumConstructor) return false
        val initialEnumClassId = initialType.fullyExpandedType().enumConstructorOwnerClassIdOrNull()
            ?: return false
        val expectedEnumClassId = expectedType.fullyExpandedType().enumConstructorOwnerClassIdOrNull()
            ?: return false
        if (initialEnumClassId != expectedEnumClassId) return false

        system.addEqualityConstraintIfCompatible(initialType, expectedType, ConeExpectedTypeConstraintPosition)
        return true
    }

    /**
     * 无参 enum constructor 的官方语义是目标类型直接定型。
     *
     * 普通约束求解不适合 `None` -> `Option<T>` 这种目标类型仍含声明类型参数的场景，
     * 因为 fresh owner 变量会被当作“未能推断”处理；官方前端在这里直接把表达式
     * 类型设置为目标 enum 类型。
     */
    private fun Candidate.noArgEnumConstructorTargetTypeSubstitutor(
        initialType: ConeCangJieType,
        resolutionMode: ResolutionMode,
    ): ConeSubstitutor? {
        val enumConstructor = symbol.takeIf { it.isBound }?.cfir as? CfirEnumConstructor ?: return null
        if (enumConstructor.valueParameters.isNotEmpty()) return null
        if (callInfo.hasExplicitTypeArguments) return null

        val enumConstructorSymbol = symbol as? CfirEnumConstructorSymbol ?: return null
        val ownerClassId = session.cfirProvider.getContainingClass(enumConstructorSymbol)?.classId ?: return null
        val initialEnumClassId = initialType.fullyExpandedType().enumConstructorOwnerClassIdOrNull() ?: return null
        if (initialEnumClassId != ownerClassId) return null

        val targetType = enumConstructorTargetType(ownerClassId, resolutionMode) ?: return null
        val targetTypeArguments = targetType.enumTypeArgumentsForClassId(ownerClassId) ?: return null
        if (targetTypeArguments.size != freshVariables.size) return null
        if (freshVariables.isEmpty()) return ConeSubstitutor.Empty

        return CfirTypeSubstitutorByMap(
            freshVariables.zip(targetTypeArguments).associate { (variable, typeArgument) ->
                variable.typeConstructor to typeArgument
            }
        )
    }

    /**
     * enum constructor 既可以由外层 expected type 定型，也可以由
     * `Option<Int>.None` 这类 member access 的显式 enum owner 类型定型。
     * 后者是仓颉 enum sugar 的真实语义，不属于调用解析兜底。
     */
    private fun Candidate.enumConstructorTargetType(
        ownerClassId: ClassId,
        resolutionMode: ResolutionMode,
    ): ConeCangJieType? {
        val expectedType = (resolutionMode as? ResolutionMode.WithExpectedType)
            ?.expectedType
            ?.fullyExpandedType()
            ?.takeIf { it.enumConstructorOwnerClassIdOrNull() == ownerClassId }
        if (expectedType != null) return expectedType

        return callInfo.explicitReceiver
            ?.coneTypeOrNull
            ?.fullyExpandedType()
            ?.takeIf { it.enumConstructorOwnerClassIdOrNull() == ownerClassId }
    }

    /**
     * enum 构造器的 owner 泛型只能从同一个 enum 的期望类型中推断。
     * 若期望类型属于其它 enum/非 enum，官方 enum sugar 路径不会把该期望类型
     * 注入构造器泛型约束，而是保留构造器自身类型，后续再报告裸泛型或类型不匹配。
     */
    private fun Candidate.shouldUseExpectedTypeForCompletion(
        initialType: ConeCangJieType,
        expectedType: ConeCangJieType,
    ): Boolean {
        if (symbol.takeIf { it.isBound }?.cfir !is CfirEnumConstructor) return true
        val initialEnumClassId = initialType.fullyExpandedType().enumConstructorOwnerClassIdOrNull() ?: return true
        val expectedEnumClassId = expectedType.fullyExpandedType().enumConstructorOwnerClassIdOrNull() ?: return false
        return initialEnumClassId == expectedEnumClassId
    }

    private fun ConeCangJieType.enumConstructorOwnerClassIdOrNull(): ClassId? = when (this) {
        is ConeEnumType -> classId
        is ConeClassLikeType -> classId.takeIf { it == StdlibClassIds.Option }
        else -> null
    }

    private fun ConeCangJieType.enumTypeArgumentsForClassId(classId: ClassId): List<ConeCangJieType>? = when (this) {
        is ConeEnumType -> typeArguments.map { it.type }.takeIf { this.classId == classId }
        is ConeClassLikeType -> typeArguments.map { it.type }.takeIf { this.classId == classId }
        else -> null
    }

    private fun Candidate.isSyntheticFunctionCallThatShouldUseEqualityConstraint(
        expectedType: ConeCangJieType,
    ): Boolean {
        if (components.context.isInsideAssignmentRhs) return false
        val symbol = symbol as? CfirCallableSymbol ?: return false
        return symbol.origin == CfirDeclarationOrigin.Synthetic.FakeFunction && !expectedType.isUnitOrAny()
    }

    private fun ConeCangJieType.isUnitOrAny(): Boolean {
        return with(session.typeContext) {
            this@isUnitOrAny.isUnit() || this@isUnitOrAny == ConeAnyType
        }
    }

    fun <T> runCompletionForCall(
        candidate: Candidate,
        completionMode: ConstraintSystemCompletionMode,
        call: T,
        initialType: ConeCangJieType,
        analyzer: PostponedArgumentsAnalyzer? = null,
    ) where T : CfirExpression, T : CfirResolvable {
        val actualAnalyzer = analyzer ?: createPostponedArgumentsAnalyzer(transformer.resolutionContext)
        completer.complete(
            candidate.system.asConstraintSystemCompleterContext(),
            completionMode,
            listOf(ConeAtomWithCandidate(call, candidate)),
            initialType,
            transformer.resolutionContext,
        ) { atom, withPCLASession ->
            actualAnalyzer.analyze(candidate.system, atom, candidate, withPCLASession)
        }
    }

    fun prepareLambdaAtomForFactoryPattern(
        atom: ConeResolvedLambdaAtom,
        candidate: Candidate,
    ) {
        val expectedFunctionType = atom.expectedType as? ConeFunctionType ?: return
        val returnVariable = ConeTypeVariableForLambdaReturnType(
            atom.anonymousFunction,
            PostponedArgumentInputTypesResolver.TYPE_VARIABLE_NAME_FOR_LAMBDA_RETURN_TYPE,
        )
        val csBuilder = candidate.system.getBuilder()
        csBuilder.registerVariable(returnVariable)

        val revisedExpectedType = ConeFunctionType(
            parameterTypes = expectedFunctionType.parameterTypes,
            returnType = returnVariable.defaultType,
            isCFunc = expectedFunctionType.isCFunc,
            isClosureType = expectedFunctionType.isClosureType,
            hasVariableLenArg = expectedFunctionType.hasVariableLenArg,
            attributes = expectedFunctionType.attributes,
        )

        csBuilder.addSubtypeConstraint(
            revisedExpectedType,
            expectedFunctionType,
            ConeArgumentConstraintPosition(atom.anonymousFunction),
        )
        atom.replaceExpectedType(revisedExpectedType, returnVariable.defaultType)
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
            components.context,
            mode,
        )
    }

    fun completedResultType(candidate: Candidate): ConeCangJieType = candidate.substitutedReturnType()

    fun createPostponedArgumentsAnalyzer(context: ResolutionContext): PostponedArgumentsAnalyzer {
        return PostponedArgumentsAnalyzer(
            context,
            LambdaAnalyzerImpl(),
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
            val lambda = lambdaAtom.anonymousFunction
            val expectedFunctionType = lambdaAtom.expectedType as? ConeFunctionType
            lambda.replaceMatchingParameterFunctionType(lambdaAtom.expectedType)
            rewriteLambdaParameterTypes(lambda.valueParameters, parameters, candidate, withPCLASession)

            val expectedReturnTypeRef = expectedReturnType?.let { returnType ->
                lambda.returnTypeRef.resolvedTypeFromPrototype(
                    returnType,
                    lambda.source?.fakeElement(CjFakeSourceElementKind.ImplicitTypeRef),
                )
            }
            if (expectedReturnTypeRef != null) {
                lambda.replaceReturnTypeRef(expectedReturnTypeRef)
            }

            /**
             * 只有当 lambda 返回类型已经被当前约束系统定到“可用 expected type”时，
             * 才把整个函数类型下传给 lambda body。
             *
             * 若这里把尚未固定的 `lambdaAtom.returnType` 也强行塞进 expected type，
             * builder-inference 场景会过早把 lambda body 压成
             * `ARGUMENT_TYPE_MISMATCH` / `CANNOT_INFER_PARAMETER_TYPE`，
             * 而不是继续让返回值约束反向流回外层调用。
             */
            val resolutionMode = expectedReturnType
                ?.let { returnType ->
                    org.cangnova.cangjie.cfir.resolve.withExpectedType(
                        ConeFunctionType(
                            parameterTypes = parameters,
                            returnType = returnType,
                            isCFunc = expectedFunctionType?.isCFunc ?: false,
                            isClosureType = expectedFunctionType?.isClosureType ?: false,
                            hasVariableLenArg = expectedFunctionType?.hasVariableLenArg ?: false,
                            attributes = expectedFunctionType?.attributes ?: org.cangnova.cangjie.cfir.types.ConeAttributes.Empty,
                        ),
                    )
                }
                ?: ResolutionMode.ContextDependent
            var additionalConstraints: ConstraintStorage? = null

            transformer.context.withAnonymousFunctionTowerDataContext(lambda.symbol) {
                val lambdaExpression = lambdaAtom.expression as CfirAnonymousFunctionExpression
                val declarationsTransformer = transformer.declarationsTransformer
                val pclaInferenceSession = runIf(withPCLASession) {
                    candidate.lambdasAnalyzedWithPCLA += lambda
                    CfirPCLAInferenceSession(candidate, session.inferenceComponents)
                }

                if (pclaInferenceSession != null) {
                    transformer.context.withInferenceSession(pclaInferenceSession) {
                        declarationsTransformer.doTransformAnonymousFunctionBodyFromCallCompletion(
                            lambdaExpression,
                            expectedReturnTypeRef,
                            resolutionMode,
                        )
                    }
                } else {
                    additionalConstraints = transformer.context.inferenceSession.runLambdaCompletion(
                        candidate,
                        forOverloadByLambdaReturnType,
                    ) {
                        declarationsTransformer.doTransformAnonymousFunctionBodyFromCallCompletion(
                            lambdaExpression,
                            expectedReturnTypeRef,
                            resolutionMode,
                        )
                    }
                }
            }
            transformer.context.dropContextForAnonymousFunction(lambda)

            val returnArguments = components.dataFlowAnalyzer
                .returnExpressionsOfAnonymousFunction(lambda)
                .map { returnInfo ->
                    val rawAtom = ConeResolutionAtom.createRawAtom(returnInfo.expression)
                    when {
                        expectedReturnType == null -> rawAtom
                        rawAtom is ConeAtomWithCandidate -> rawAtom
                        else -> ConeSimpleLeafResolutionAtom(returnInfo.expression, allowUnresolvedExpression = false)
                    }
                }

            return ReturnArgumentsAnalysisResult(returnArguments, additionalConstraints)
        }

        private fun rewriteLambdaParameterTypes(
            parameters: List<CfirValueParameter>,
            inferredTypes: List<ConeCangJieType>,
            candidate: Candidate,
            withPCLASession: Boolean,
        ) {
            parameters.forEachIndexed { index, parameter ->
                if (index >= inferredTypes.size) {
                    parameter.replaceReturnTypeRef(
                        buildErrorTypeRef {
                            diagnostic = ConeCannotInferValueParameterType(
                                parameter.symbol,
                                "Lambda or anonymous function has more parameters than expected",
                            )
                            source = parameter.source
                        },
                    )
                    return@forEachIndexed
                }

                val approximated = inferredTypes[index].approximateLambdaInputType(
                    parameter.symbol,
                    withPCLASession,
                    candidate,
                )
                val source =
                    parameter.source?.fakeElement(CjFakeSourceElementKind.ImplicitReturnTypeOfLambdaValueParameter)
                val newTypeRef = if (parameter.returnTypeRef is CfirImplicitTypeRef) {
                    approximated.toResolvedTypeRef(source)
                } else {
                    parameter.returnTypeRef.resolvedTypeFromPrototype(approximated, source)
                }
                parameter.replaceReturnTypeRef(newTypeRef)
            }
        }
    }

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
            this,
            TypeApproximatorConfiguration.IntermediateApproximationToSupertypeAfterCompletionInK2,
        ) ?: this
    }

    private fun ConeCangJieType.useErrorTypeInsteadOfTypeVariableForParameterType(
        isRootLambdaForPCLASession: Boolean,
    ): Boolean {
        if (this !is org.cangnova.cangjie.cfir.types.ConeTypeVariableType) return false
        if (isRootLambdaForPCLASession || inferenceSession is CfirPCLAInferenceSession) {
            return typeConstructor.originalTypeParameter == null
        }
        return true
    }
}

private fun Candidate.isFunctionForExpectTypeFromCastFeature(): Boolean {
    if (typeArgumentMapping != TypeArgumentMapping.NoExplicitArguments) return false
    val cfir = symbol.cfir as? CfirFunction ?: return false
    return cfir.isFunctionForExpectTypeFromCastFeature()
}

internal fun CfirFunction.isFunctionForExpectTypeFromCastFeature(): Boolean {
    val typeParameter = typeParameters.singleOrNull() ?: return false
    val returnType = returnTypeRef.coneTypeSafe<ConeCangJieType>() ?: return false

    if ((returnType.unwrap() as? ConeTypeParameterType)?.lookupTag != typeParameter.symbol.toLookupTag()) {
        return false
    }

    fun CfirTypeRef.isBadType(): Boolean {
        return coneTypeSafe<ConeCangJieType>()
            ?.contains {
                (it.unwrap() as? ConeTypeParameterType)?.lookupTag == typeParameter.symbol.toLookupTag()
            } != false
    }

    return valueParameters.none { it.returnTypeRef.isBadType() }
}
fun CfirLookupTrackerComponent.recordClassLikeLookup(classId: ClassId, source: CjSourceElement?, fileSource: CjSourceElement?) {
//TODO 排除基本类型
//    if ( classId !in StandardClassIds.allBuiltinTypes) {
//        val classFqName = classId.asSingleFqName()
//        recordLookup(classFqName.shortName().asString(), classFqName.parent().asString(), source, fileSource)
//    }
}
fun CfirLookupTrackerComponent.recordTypeResolveAsLookup(
    type: ConeCangJieType?,
    source: CjSourceElement?,
    fileSource: CjSourceElement?,
) {
    if (type == null) return
    if (source == null && fileSource == null) return // TODO: investigate all cases
    if (type is ConeErrorType) return // TODO: investigate whether some cases should be recorded, e.g. unresolved
    type.classId?.let { classId ->
        recordClassLikeLookup(classId, source, fileSource)
    }
    type.typeArguments.forEach {
        recordTypeResolveAsLookup(it.type, source, fileSource)
    }
}

private fun CfirTypeRef.resolvedTypeFromPrototype(
    type: ConeCangJieType,
    source: CjSourceElement?,
): CfirResolvedTypeRef {
    return when (type) {
        is ConeErrorType -> buildErrorTypeRef {
            this.source = source ?: this@resolvedTypeFromPrototype.source
            coneType = type
            delegatedTypeRef = this@resolvedTypeFromPrototype
            diagnostic = type.diagnostic
        }

        else -> buildResolvedTypeRef {
            this.source = source ?: this@resolvedTypeFromPrototype.source
            coneType = type
            delegatedTypeRef = this@resolvedTypeFromPrototype
        }
    }
}

private fun ConeCangJieType.toResolvedTypeRef(source: CjSourceElement?): CfirResolvedTypeRef {
    return when (this) {
        is ConeErrorType -> buildErrorTypeRef {
            this.source = source
            coneType = this@toResolvedTypeRef
            diagnostic = this@toResolvedTypeRef.diagnostic
        }

        else -> buildResolvedTypeRef {
            this.source = source
            coneType = this@toResolvedTypeRef
        }
    }
}

private fun ConeCangJieType.unwrap(): ConeSimpleCangJieType = this as ConeSimpleCangJieType
