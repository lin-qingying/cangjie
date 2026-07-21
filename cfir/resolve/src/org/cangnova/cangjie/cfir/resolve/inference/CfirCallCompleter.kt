package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirLookupTrackerComponent
import org.cangnova.cangjie.cfir.SessionHolder
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.diagnostic.ConeCannotInferValueParameterType
import org.cangnova.cangjie.cfir.diagnostic.ConeUnableToInferExpressionTypeError
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
import org.cangnova.cangjie.cfir.resolve.calls.substituteExplicitTypeArgumentConstraints
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeDeclaredUpperBoundConstraintPosition
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeExplicitTypeParameterConstraintPosition
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeArgumentConstraintPosition
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeExpectedTypeConstraintPosition
import org.cangnova.cangjie.cfir.resolve.initialTypeOfCandidate
import org.cangnova.cangjie.cfir.resolve.toSymbol
import org.cangnova.cangjie.cfir.resolve.toErrorReference
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.CfirPCLAInferenceSession
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.resultType
import org.cangnova.cangjie.cfir.resolve.typeFromCallee
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeVariableForLambdaReturnType
import org.cangnova.cangjie.cfir.scopes.impl.typeAliasConstructorInfo
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeSubstitutorByMap
import org.cangnova.cangjie.cfir.types.ConeAnyType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeClassifierType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConeLookupTagBasedType
import org.cangnova.cangjie.cfir.types.ConePointerType
import org.cangnova.cangjie.cfir.types.ConeRigidType
import org.cangnova.cangjie.cfir.types.ConeSimpleCangJieType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.cfir.types.ConeUnreportedDuplicateDiagnostic
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.types.arrayLiteralElementType
import org.cangnova.cangjie.cfir.types.asCone
import org.cangnova.cangjie.cfir.types.builder.buildErrorTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.cfir.types.classId
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.coneTypeSafe
import org.cangnova.cangjie.cfir.types.contains
import org.cangnova.cangjie.cfir.types.createTypeSubstitutorByTypeConstructor
import org.cangnova.cangjie.cfir.types.expandedClassIdOrPrimitiveClassId
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
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintMismatch
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.fakeElement
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.type.model.TypeConstructorMarker
import org.cangnova.cangjie.types.TypeApproximatorConfiguration
import org.cangnova.cangjie.utils.runIf

/**
 * CFIR 调用完成器。
 *
 * 该组件位于函数体解析与约束系统之间，负责把候选调用在 tower resolve 阶段收集到的约束
 * 推进到完整或部分完成状态，并把最终替换结果写回调用表达式、lambda 参数、返回类型以及 lookup tracker。
 */
class CfirCallCompleter(
    /** 当前函数体解析调度器，用于访问 body resolve 上下文、声明转换器和当前推断会话。 */
    private val transformer: CfirAbstractBodyResolveTransformerDispatcher,
    /** 调用完成所依赖的 body resolve 组件集合，包括 session、scope、数据流分析器与类型近似器。 */
    private val components: CfirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
) : SessionHolder {
    /** 当前 CFIR 会话，作为类型上下文、provider、lookup tracker 和推断组件的统一入口。 */
    override val session: CfirSession = components.session

    /** 当前 body resolve 上下文中的推断会话，用于处理 PCLA 与部分完成调用。 */
    private val inferenceSession
        get() = transformer.context.inferenceSession

    /** 约束系统完成器，执行固定类型变量、分析 postponed atom 与构造最终 substitutor 的核心流程。 */
    val completer: ConstraintSystemCompleter = ConstraintSystemCompleter(components)

    /**
     * 完成一个已经选出候选的 CFIR 调用表达式。
     *
     * 方法会先写入候选初始返回类型，再根据期望类型、enum sugar、typealias 构造器、
     * synthetic function 和 PCLA 场景补充约束；随后按候选计算出的完成模式执行完整或部分完成。
     * 返回值保持原表达式类型，必要时通过 completion writer 原地替换其中的类型引用。
     */
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

        /*
         * 裸 generic enum owner 可能先把候选标记为恢复型 error reference，但无参 constructor
         * 仍可由同一 owner 的 expected type 或显式 qualifier 完整定型。该结构化 substitution
         * 必须先于普通错误候选早退；无法覆盖全部 fresh owner 变量时 helper 返回 null，
         * 其余错误候选仍保持不可完成。
         */
        when (val completion = candidate.noArgEnumConstructorTargetCompletion(initialType, resolutionMode)) {
            is NoArgEnumConstructorCompletion.Resolved -> {
                val completedCall = call.transformSingle(createCompletionResultsWriter(completion.substitutor), null)
                completedCall.replaceConeTypeOrNull(completion.targetType)
                return completedCall
            }

            NoArgEnumConstructorCompletion.UnableToInferExpectedOwner -> {
                val diagnostic = ConeUnableToInferExpressionTypeError()
                call.replaceCalleeReference(reference.toErrorReference(diagnostic))
                call.resultType = ConeErrorType(
                    ConeUnreportedDuplicateDiagnostic(diagnostic),
                    delegatedType = initialType,
                )
                return call
            }

            null -> Unit
        }

        /*
         * 不适用候选只用于承载失败诊断，不能经过 completion 被写成 resolved reference。
         * 但调用结果必须先成为错误类型，使外围类型检查和成员解析识别这是既有错误的级联。
         */
        if (reference.isError) return call

        session.lookupTracker?.recordTypeResolveAsLookup(initialType, call.source, components.context.file.source)
        if (candidate.shouldKeepNoArgEnumConstructorForBareGenericDiagnostic(initialType)) {
            return call.transformSingle(createCompletionResultsWriter(ConeSubstitutor.Empty), null)
        }
        addConstraintFromExpectedType(candidate, initialType, resolutionMode)
        candidate.addSameClassifierArgumentTypeConstraints()

        if (skipEvenPartialCompletion) return call

        val computedCompletionMode = if (
            components.context.isInsideCallArgumentResolution &&
            resolutionMode is ResolutionMode.ContextDependent &&
            candidate.containsSystemNotFixedVariable(candidate.substitutedReturnType())
        ) {
            ConstraintSystemCompletionMode.PARTIAL
        } else {
            candidate.computeCompletionMode(
                session.inferenceComponents,
                resolutionMode,
                initialType,
            )
        }
        val completionMode = when {
            candidate.isSyntheticCallForTopLevelLambdaWithoutExpectedFunctionType() ->
                ConstraintSystemCompletionMode.PCLA_POSTPONED_CALL

            else -> computedCompletionMode.let {
                when {
                    it == ConstraintSystemCompletionMode.FULL ->
                        inferenceSession.customCompletionModeInsteadOfFull(call) ?: ConstraintSystemCompletionMode.FULL
                    else -> it
                }
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
                // PARTIAL 的顶层类型必须保留候选返回签名中的结构化 owner 变量。
                // 使用已写回表达式类型会把 enum constructor 的 owner 提前物化，
                // 使外层 expected type 无法继续反向约束当前候选。
                val completionTopLevelType = candidate.substitutedReturnType()
                runCompletionForCall(candidate, completionMode, call, completionTopLevelType, analyzer)
                call.updatePartiallyCompletedResultType(candidate)
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

    /**
     * 嵌套调用在部分完成后只需要把当前可确定的返回类型暴露给外层候选。
     *
     * 这里不运行完整 completion writer：候选引用、实参树和 postponed atom 仍由最终选中
     * 候选的完成阶段写回；当前步骤只修正 `foo(id(1))` 这类外层实参检查读取到的
     * 返回类型，避免把内层 fresh type variable 当成普通实参类型。
     */
    private fun CfirExpression.updatePartiallyCompletedResultType(candidate: Candidate) {
        if (this is CfirAnnotationCall || this is CfirAnonymousFunctionExpression) return
        val currentType = candidate.substitutedReturnType()
        val substitutor = candidate.system.currentStorage()
            .buildCurrentSubstitutor(session.typeContext, emptyMap())
            .asCone()
        val updatedType = substitutor.substituteOrNull(currentType) ?: return
        replaceConeTypeOrNull(updatedType)
    }

    /**
     * 判断候选是否是承载顶层 lambda 语义的 synthetic call。
     *
     * 这类调用在仓颉中用于让无上下文 lambda 继续通过成员访问和外层调用反推参数类型，
     * 因此部分完成后也需要写回当前 substitutor。
     */
    private fun Candidate.isSyntheticCallForTopLevelLambda(): Boolean =
        callInfo.callSite is CfirAnonymousFunctionExpression

    /**
     * 无上下文 lambda 的 synthetic accept 不能在 initializer 阶段做 FULL completion。
     *
     * 官方 SynLamExpr 会保留形参 placeholder，让 body 中的成员访问/调用语法和后续使用点继续约束它；
     * 若这里把 `Any` 参数位当成完整上下文，会把所有未推断输入变量立即固定成错误类型。
     */
    private fun Candidate.isSyntheticCallForTopLevelLambdaWithoutExpectedFunctionType(): Boolean {
        if (!isSyntheticCallForTopLevelLambda()) return false
        val function = symbol.takeIf { it.isBound }?.cfir as? CfirFunction ?: return false
        val parameterType = function.valueParameters.singleOrNull()
            ?.returnTypeRef
            ?.coneTypeOrNull
        return parameterType !is ConeFunctionType
    }

    /**
     * 从解析模式中的期望类型向候选约束系统注入外层上下文约束。
     *
     * 这里统一处理普通 subtype 约束、cast 语境、synthetic function 的 equality 约束、
     * 最后一条 Unit 语句、内建数组/指针构造器、enum constructor 以及 typealias 构造器的错误抑制边界。
     */
    private fun addConstraintFromExpectedType(
        candidate: Candidate,
        initialType: ConeCangJieType,
        resolutionMode: ResolutionMode,
    ) {
        if (resolutionMode !is ResolutionMode.WithExpectedType) return
        val expectedType = resolutionMode.expectedType.fullyExpandedType()
        if (!candidate.shouldUseExpectedTypeForCompletion(initialType, expectedType)) return
        if (
            resolutionMode.lastStatementInBlock &&
            candidate.system.isProperType(initialType)
        ) {
            // 声明体尾表达式的确定返回类型由 body 类型检查与声明返回类型比较，不能再反向写入
            // 已经合法解析的调用候选。否则 `print(value): Unit` 会因外层 `main(): Int64`
            // 注入矛盾约束并退化成 NEW_INFERENCE_ERROR，而不是在完整调用上报告 TYPE_MISMATCH。
            // 返回类型含未固定变量（例如 `make<T>(): T`）时仍使用 expected type 完成推断；
            // 数组、enum 等依赖目标类型的构造调用同样保留原有推断路径。
            return
        }
        if (
            candidate.hasTypeAliasConstructorExpansionUpperBoundViolation() ||
            candidate.hasTypeAliasConstructorUpperBoundMismatchBeforeExpectedType()
        ) {
            return
        }
        val system = candidate.system

        if (candidate.addBuiltinArrayConstructorExpectedElementConstraint(expectedType)) return
        if (candidate.addBuiltinPointerConstructorExpectedPointeeConstraint(expectedType)) return
        if (candidate.addEnumConstructorExpectedTypeConstraint(initialType, expectedType)) return
        if (candidate.addEnumConstructorPayloadExpectedTypeConstraint(initialType, expectedType)) return

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
     * 官方 `CPointer()` 在无显式类型实参时可从目标 `CPointer<T>` 反推 pointee 类型。
     *
     * 目标为 `CType` 或某个 extend 接口时不能直接反推出 pointee；这些场景仍交给
     * 普通子类型/extend 约束处理，避免把所有 C pointer 退化成同一个泛型实参。
     */
    private fun Candidate.addBuiltinPointerConstructorExpectedPointeeConstraint(
        expectedType: ConeCangJieType,
    ): Boolean {
        val callable = symbol.takeIf { it.isBound }?.cfir as? CfirFunction ?: return false
        if (callable.origin != CfirDeclarationOrigin.Synthetic.BuiltinPointerConstructor) return false
        if (callInfo.hasExplicitTypeArguments) return false

        val expectedPointerType = expectedType.fullyExpandedType() as? ConePointerType ?: return false
        val pointeeVariableType = freshVariables.singleOrNull()?.defaultType as? ConeCangJieType ?: return false
        system.addSubtypeConstraint(pointeeVariableType, expectedPointerType.pointeeType, ConeExpectedTypeConstraintPosition)
        return true
    }

    /**
     * 检测 typealias 构造器展开后的真实 class 类型实参是否已经违反声明上界。
     *
     * 显式类型实参先映射到 typealias 暴露的参数，再展开到真实构造器 owner；
     * 该检查用于在 expected-type 约束进入前识别已经由显式类型实参 checker 负责的上界错误。
     */
    private fun Candidate.hasTypeAliasConstructorExpansionUpperBoundViolation(): Boolean {
        val constructorSymbol = symbol as? CfirConstructorSymbol ?: return false
        val typeAliasConstructorInfo = constructorSymbol.typeAliasConstructorInfo ?: return false
        if (typeArgumentMapping == TypeArgumentMapping.NoExplicitArguments) return false

        val typeAlias = typeAliasConstructorInfo.typeAliasSymbol.cfir
        val typeArguments = typeAlias.typeParameters.indices.map { index ->
            typeArgumentMapping[index]
        }
        if (typeArguments.isEmpty()) return false

        val expandedType = ConeTypeAliasType(
            classId = typeAliasConstructorInfo.typeAliasSymbol.classId,
            expandedType = typeAlias.expandedTypeRef.coneTypeSafe<ConeCangJieType>(),
            typeArguments = typeArguments,
        ).fullyExpandedType(session) as? ConeClassifierType ?: return false
        if (expandedType.typeArguments.isEmpty()) return false

        val expandedSymbol = expandedType.toSymbol(session) as? CfirClassLikeSymbol<*> ?: return false
        val typeParameters = expandedSymbol.cfir.typeParameters
        if (typeParameters.isEmpty()) return false

        val substitutor = createTypeSubstitutorByTypeConstructor(
            map = typeParameters
                .zip(expandedType.typeArguments.map { it.type })
                .associate { (typeParameter, argument) ->
                    typeParameter.symbol.toLookupTag() as TypeConstructorMarker to argument
                },
            context = session.typeContext,
            approximateIntegerLiterals = false,
        )

        val count = minOf(typeParameters.size, expandedType.typeArguments.size)
        for (index in 0 until count) {
            val argumentType = expandedType.typeArguments[index].type
            if (argumentType is ConeErrorType) continue

            val upperBounds = typeParameters[index].symbol.resolvedBounds
                .map { it.coneType }
                .filterNot { it is ConeErrorType }
            if (upperBounds.isEmpty()) continue

            val upperBound = substitutor.substituteOrSelf(
                session.typeContext.intersectTypes(upperBounds) as ConeCangJieType,
            )
            if (upperBound !is ConeErrorType &&
                !AbstractTypeChecker.isSubtypeOfWithoutOptionBoxing(
                    session.typeContext,
                    argumentType,
                    upperBound,
                )
            ) {
                return true
            }
        }
        return false
    }

    /**
     * typealias 构造器的展开上界错误已经由显式类型实参 checker 报告在别名名处。
     * 此时继续加入 expected-type 约束只会把同一错误级联成外层 TYPE_MISMATCH。
     */
    private fun Candidate.hasTypeAliasConstructorUpperBoundMismatchBeforeExpectedType(): Boolean {
        val constructorSymbol = symbol as? CfirConstructorSymbol ?: return false
        if (constructorSymbol.typeAliasConstructorInfo == null) return false
        if (!callInfo.hasExplicitTypeArguments) return false

        return system.errors.any { error ->
            error is ConstraintMismatch &&
                when (error.position.from) {
                    is ConeExplicitTypeParameterConstraintPosition,
                    is ConeDeclaredUpperBoundConstraintPosition,
                    -> true

                    else -> false
                }
        }
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
        val enumConstructor = symbol.takeIf { it.isBound }?.cfir as? CfirEnumConstructor ?: return false
        if (enumConstructor.valueParameters.isNotEmpty()) return false
        val initialEnumClassId = initialType.fullyExpandedType().enumConstructorOwnerClassIdOrNull()
            ?: return false
        val expectedEnumClassId = expectedType.fullyExpandedType().enumConstructorOwnerClassIdOrNull()
            ?: return false
        if (initialEnumClassId != expectedEnumClassId) return false

        system.addEqualityConstraintIfCompatible(initialType, expectedType, ConeExpectedTypeConstraintPosition)
        return true
    }

    /**
     * 带 payload 的 enum constructor 需要把目标 owner 类型投影到 payload 泛型参数。
     *
     * 官方 `Some(a): ??I` 会同时形成 `?A <: T` 与 `T <: ?I`。这不是初始化器层面的
     * 普通类型不匹配，而是隐式 enum constructor 调用的泛型实参无法求解；只有当
     * payload 下界和目标上界是同构 nominal 类型且内部实参存在 subtype 关系时，才把
     * 目标 payload 作为推断上界加入，避免把形状完全不同的目标类型误归为推断失败。
     */
    private fun Candidate.addEnumConstructorPayloadExpectedTypeConstraint(
        initialType: ConeCangJieType,
        expectedType: ConeCangJieType,
    ): Boolean {
        val enumConstructor = symbol.takeIf { it.isBound }?.cfir as? CfirEnumConstructor ?: return false
        if (enumConstructor.valueParameters.isEmpty()) return false
        if (callInfo.hasExplicitTypeArguments) return false
        if (!argumentMappingInitialized) return false

        val ownerClassId = initialType.fullyExpandedType().enumConstructorOwnerClassIdOrNull() ?: return false
        if (expectedType.fullyExpandedType().enumConstructorOwnerClassIdOrNull() != ownerClassId) return false

        val initialTypeArguments = initialType
            .fullyExpandedType(session)
            .enumTypeArgumentsForClassId(ownerClassId)
            ?: return false
        val expectedTypeArguments = expectedType
            .fullyExpandedType(session)
            .enumTypeArgumentsForClassId(ownerClassId)
            ?: return false
        if (initialTypeArguments.size != expectedTypeArguments.size) return false

        var addedConstraint = false
        for ((initialArgument, expectedArgument) in initialTypeArguments.zip(expectedTypeArguments)) {
            if (!containsSystemNotFixedVariable(initialArgument)) continue

            for ((atom, parameter) in argumentMapping) {
                val parameterType = parameter.returnTypeRef.coneTypeOrNull
                    ?.let(substitutor::substituteOrSelf)
                    ?: continue
                if (!parameterType.isSameCandidateVariable(initialArgument)) continue

                val argumentType = atom.expression.coneTypeOrNull ?: continue
                if (!argumentType.shouldUseAsEnumPayloadInferenceLowerBound(expectedArgument)) continue

                system.addSubtypeConstraint(
                    argumentType,
                    expectedArgument,
                    ConeArgumentConstraintPosition(atom.expression),
                )
                addedConstraint = true
            }
        }
        return addedConstraint
    }

    /** 判断参数类型是否正是 enum owner 的当前 fresh 变量。 */
    private fun ConeCangJieType.isSameCandidateVariable(other: ConeCangJieType): Boolean {
        val left = this as? ConeTypeVariableType ?: return false
        val right = other as? ConeTypeVariableType ?: return false
        return left.typeConstructor == right.typeConstructor
    }

    /**
     * 判断 payload 下界是否应与目标 payload 上界组成推断失败约束。
     *
     * 两者根 classifier 必须相同且内部实参存在真实 subtype 关系；形状不一致的目标
     * 继续交给初始化器类型检查，保持官方 `???I = Some(a)` 的普通 mismatch 口径。
     */
    private fun ConeCangJieType.shouldUseAsEnumPayloadInferenceLowerBound(
        expectedPayloadType: ConeCangJieType,
    ): Boolean {
        if (AbstractTypeChecker.isSubtypeOf(session.typeContext, this, expectedPayloadType) == true) return false
        val actual = fullyExpandedType(session) as? ConeLookupTagBasedType ?: return false
        val expected = expectedPayloadType.fullyExpandedType(session) as? ConeLookupTagBasedType ?: return false
        if (actual.expandedClassIdOrPrimitiveClassId != expected.expandedClassIdOrPrimitiveClassId) return false
        if (actual.typeArguments.size != expected.typeArguments.size) return false

        var hasRelatedDifference = false
        for ((actualArgument, expectedArgument) in actual.typeArguments.zip(expected.typeArguments)) {
            val actualArgumentType = actualArgument.type
            val expectedArgumentType = expectedArgument.type
            if (actualArgumentType == expectedArgumentType) continue
            if (actualArgumentType.isOptionShape() != expectedArgumentType.isOptionShape()) return false
            val related = AbstractTypeChecker.isSubtypeOf(session.typeContext, actualArgumentType, expectedArgumentType) == true ||
                    AbstractTypeChecker.isSubtypeOf(session.typeContext, expectedArgumentType, actualArgumentType) == true
            if (!related) return false
            hasRelatedDifference = true
        }
        return hasRelatedDifference
    }

    /** 判断类型展开后是否为标准库 Option 形状。 */
    private fun ConeCangJieType.isOptionShape(): Boolean =
        (fullyExpandedType(session) as? ConeLookupTagBasedType)
            ?.expandedClassIdOrPrimitiveClassId == StdlibClassIds.Option

    /**
     * 从已完成实参映射中提取同构泛型类型实参约束。
     *
     * 这一步属于 LocalTypeArgumentSynthesis 的 completion 输入：`Array<Int64>` 对 `Array<T>`
     * 不只是整体 subtype 检查，还要把 `T == Int64` 写入候选约束系统。它必须发生在
     * 固定类型变量之前，才能让 enum constructor receiver owner、普通泛型函数和返回类型
     * 使用同一组最终 substitutor。
     */
    private fun Candidate.addSameClassifierArgumentTypeConstraints() {
        if (!argumentMappingInitialized) return
        for ((atom, parameter) in argumentMapping) {
            val argumentType = atom.expression.coneTypeOrNull ?: continue
            val expectedType = parameter.returnTypeRef.coneTypeOrNull
                ?.let(substitutor::substituteOrSelf)
                ?.let(::substituteExplicitTypeArgumentConstraints)
                ?: continue
            addSameClassifierTypeArgumentConstraints(
                argumentType = argumentType,
                expectedType = expectedType,
                position = ConeArgumentConstraintPosition(atom.expression),
            )
        }
    }

    /** 对同一个 classifier 的类型实参递归登记 equality 约束。 */
    private fun Candidate.addSameClassifierTypeArgumentConstraints(
        argumentType: ConeCangJieType,
        expectedType: ConeCangJieType,
        position: ConeArgumentConstraintPosition,
    ) {
        val currentStorage = system.currentStorage()
        val currentSubstitutor = currentStorage
            .buildCurrentSubstitutor(session.typeContext, emptyMap())
            .asCone()
        val substitutedArgumentType = currentSubstitutor.substituteOrNull(argumentType) ?: argumentType
        val substitutedExpectedType = currentSubstitutor.substituteOrNull(expectedType) ?: expectedType
        val actualClassifier = substitutedArgumentType.fullyExpandedType(session) as? ConeLookupTagBasedType ?: return
        val expectedClassifier = substitutedExpectedType.fullyExpandedType(session) as? ConeLookupTagBasedType ?: return
        if (actualClassifier.expandedClassIdOrPrimitiveClassId != expectedClassifier.expandedClassIdOrPrimitiveClassId) {
            return
        }
        if (actualClassifier.typeArguments.size != expectedClassifier.typeArguments.size) return
        val ownedNotFixedConstructors = currentStorage.notFixedTypeVariables.keys

        for ((actualArgument, expectedArgument) in actualClassifier.typeArguments.zip(expectedClassifier.typeArguments)) {
            val actualArgumentType = actualArgument.type
            val expectedArgumentType = expectedArgument.type
            if (!containsSystemNotFixedVariable(expectedArgumentType)) continue
            /*
             * 同构分解只能约束当前 candidate storage 拥有的 fresh constructors。
             * 嵌套调用/subcandidate 的变量会随 atom subsystem 合并或最终 substitutor 物化；
             * 在此之前把 foreign constructor 送入当前 ConstraintInjector 会破坏系统所有权不变量。
             */
            if (actualArgumentType.containsForeignInferenceVariable(ownedNotFixedConstructors) ||
                expectedArgumentType.containsForeignInferenceVariable(ownedNotFixedConstructors)
            ) {
                continue
            }
            system.addEqualityConstraintIfCompatible(actualArgumentType, expectedArgumentType, position)
            addSameClassifierTypeArgumentConstraints(actualArgumentType, expectedArgumentType, position)
        }
    }

    /** 判断类型树是否含不属于当前候选 storage 的 inference variable。 */
    private fun ConeCangJieType.containsForeignInferenceVariable(
        ownedNotFixedConstructors: Set<TypeConstructorMarker>,
    ): Boolean = when (this) {
        is ConeTypeVariableType -> typeConstructor !in ownedNotFixedConstructors
        is ConeLookupTagBasedType -> typeArguments.any { it.type.containsForeignInferenceVariable(ownedNotFixedConstructors) }
        is ConeFunctionType -> parameterTypes.any { it.containsForeignInferenceVariable(ownedNotFixedConstructors) } ||
                returnType.containsForeignInferenceVariable(ownedNotFixedConstructors)
        is ConeTupleType -> elementTypes.any { it.containsForeignInferenceVariable(ownedNotFixedConstructors) }
        is ConeVArrayType -> elementType.containsForeignInferenceVariable(ownedNotFixedConstructors)
        is ConePointerType -> pointeeType.containsForeignInferenceVariable(ownedNotFixedConstructors)
        is ConeTypeAliasType -> typeArguments.any { it.type.containsForeignInferenceVariable(ownedNotFixedConstructors) } ||
                expandedType?.containsForeignInferenceVariable(ownedNotFixedConstructors) == true
        else -> false
    }

    /** 判断类型树是否含当前候选的 fresh type variable。 */
    private fun Candidate.containsCandidateFreshVariable(type: ConeCangJieType): Boolean = when (type) {
        is ConeTypeVariableType -> freshVariables.any { it.typeConstructor == type.typeConstructor }
        is ConeLookupTagBasedType -> type.typeArguments.any { containsCandidateFreshVariable(it.type) }
        is ConeFunctionType -> type.parameterTypes.any { containsCandidateFreshVariable(it) } ||
                containsCandidateFreshVariable(type.returnType)
        is ConeTupleType -> type.elementTypes.any { containsCandidateFreshVariable(it) }
        is ConeVArrayType -> containsCandidateFreshVariable(type.elementType)
        is ConePointerType -> containsCandidateFreshVariable(type.pointeeType)
        is ConeTypeAliasType -> type.typeArguments.any { containsCandidateFreshVariable(it.type) } ||
                type.expandedType?.let { containsCandidateFreshVariable(it) } == true
        else -> false
    }

    /** 判断类型树是否含当前候选系统中尚未固定的变量。 */
    private fun Candidate.containsSystemNotFixedVariable(type: ConeCangJieType): Boolean {
        val notFixedTypeVariables = system.currentStorage().notFixedTypeVariables.keys
        return when (type) {
            is ConeTypeVariableType -> type.typeConstructor in notFixedTypeVariables
            is ConeLookupTagBasedType -> type.typeArguments.any { containsSystemNotFixedVariable(it.type) }
            is ConeFunctionType -> type.parameterTypes.any { containsSystemNotFixedVariable(it) } ||
                    containsSystemNotFixedVariable(type.returnType)
            is ConeTupleType -> type.elementTypes.any { containsSystemNotFixedVariable(it) }
            is ConeVArrayType -> containsSystemNotFixedVariable(type.elementType)
            is ConePointerType -> containsSystemNotFixedVariable(type.pointeeType)
            is ConeTypeAliasType -> type.typeArguments.any { containsSystemNotFixedVariable(it.type) } ||
                    type.expandedType?.let { containsSystemNotFixedVariable(it) } == true
            else -> false
        }
    }

    /**
     * 无参 enum constructor 的官方语义是目标类型直接定型。
     *
     * 普通约束求解不适合 `None` -> `Option<T>` 这种目标类型仍含声明类型参数的场景，
     * 因为 fresh owner 变量会被当作“未能推断”处理；官方前端在这里直接把表达式
     * 类型设置为目标 enum 类型。
     */
    private fun Candidate.noArgEnumConstructorTargetCompletion(
        initialType: ConeCangJieType,
        resolutionMode: ResolutionMode,
    ): NoArgEnumConstructorCompletion? {
        val enumConstructor = symbol.takeIf { it.isBound }?.cfir as? CfirEnumConstructor ?: return null
        if (enumConstructor.valueParameters.isNotEmpty()) return null
        if (callInfo.hasExplicitTypeArguments) return null

        val enumConstructorSymbol = symbol as? CfirEnumConstructorSymbol ?: return null
        val ownerClassId = session.cfirProvider.getContainingClass(enumConstructorSymbol)?.classId ?: return null
        val initialEnumClassId = initialType.fullyExpandedType().enumConstructorOwnerClassIdOrNull() ?: return null
        if (initialEnumClassId != ownerClassId) return null

        val targetType = when (val target = enumConstructorTargetType(initialType, ownerClassId, resolutionMode)) {
            is EnumConstructorTargetTypeResult.Resolved -> target.type
            EnumConstructorTargetTypeResult.UnableToInferExpectedOwner ->
                return NoArgEnumConstructorCompletion.UnableToInferExpectedOwner
            EnumConstructorTargetTypeResult.NotApplicable -> return null
        }
        if (freshVariables.isEmpty()) {
            return NoArgEnumConstructorCompletion.Resolved(targetType, ConeSubstitutor.Empty)
        }

        val targetSubstitution = createNoArgEnumConstructorTargetSubstitution(
            initialType = initialType,
            targetType = targetType,
            ownerClassId = ownerClassId,
        ) ?: return null
        val freshTypeConstructors = freshVariables.mapTo(mutableSetOf()) { it.typeConstructor }
        if (!freshTypeConstructors.all { it in targetSubstitution }) return null

        return NoArgEnumConstructorCompletion.Resolved(targetType, CfirTypeSubstitutorByMap(targetSubstitution))
    }

    /** 无参 enum constructor 的目标完成结果。 */
    private sealed interface NoArgEnumConstructorCompletion {
        /** owner 已完整定型。 */
        data class Resolved(
            val targetType: ConeCangJieType,
            val substitutor: ConeSubstitutor,
        ) : NoArgEnumConstructorCompletion

        /** expected supertype 无法反推出全部 owner 参数。 */
        data object UnableToInferExpectedOwner : NoArgEnumConstructorCompletion
    }

    /**
     * 无目标类型的无参 enum constructor 不是“泛型函数推断失败”，而是裸泛型 owner
     * 缺少类型实参。这里保留已经解析出的 enum constructor 引用和带 fresh owner 变量的
     * 返回类型，让后续裸泛型访问 checker 统一报告 `GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT`。
     */
    private fun Candidate.shouldKeepNoArgEnumConstructorForBareGenericDiagnostic(
        initialType: ConeCangJieType,
    ): Boolean {
        val enumConstructor = symbol.takeIf { it.isBound }?.cfir as? CfirEnumConstructor ?: return false
        if (enumConstructor.valueParameters.isNotEmpty()) return false
        if (callInfo.hasExplicitTypeArguments) return false
        return containsCandidateFreshVariable(initialType)
    }

    /**
     * 根据 enum constructor 初始类型与目标类型构造 fresh 变量到目标 owner 实参的替换表。
     *
     * 返回 `null` 表示初始 owner 与目标 owner 无法按同一个 enum 结构匹配，调用完成应继续走普通约束路径。
     */
    private fun Candidate.createNoArgEnumConstructorTargetSubstitution(
        initialType: ConeCangJieType,
        targetType: ConeCangJieType,
        ownerClassId: ClassId,
    ): Map<TypeConstructorMarker, ConeCangJieType>? {
        val initialTypeArguments = initialType
            .fullyExpandedType(session)
            .enumTypeArgumentsForClassId(ownerClassId)
            ?: return null
        val targetTypeArguments = targetType
            .fullyExpandedType(session)
            .enumTypeArgumentsForClassId(ownerClassId)
            ?: return null
        if (initialTypeArguments.size != targetTypeArguments.size) return null

        val freshTypeConstructors = freshVariables.mapTo(mutableSetOf()) { it.typeConstructor }
        val substitution = linkedMapOf<TypeConstructorMarker, ConeCangJieType>()
        for ((initialArgument, targetArgument) in initialTypeArguments.zip(targetTypeArguments)) {
            if (!collectTargetSubstitutionFromMatchingTypes(
                    initialArgument,
                    targetArgument,
                    freshTypeConstructors,
                    substitution,
                )
            ) {
                return null
            }
        }
        return substitution
    }

    /**
     * 无参 enum constructor 的 target type 定型需要从 owner 返回类型中抽取 fresh 变量绑定。
     *
     * `type X<K> = E<Int32, K>` 中，调用 `X.EEE` 时参与推断的 fresh 变量只有别名暴露的 `K`，
     * 而目标 enum owner 类型仍是完整的 `E<Int32, Int8>`；因此这里按同形 owner 类型参数递归匹配，
     * 只为 fresh 变量生成替代项。
     */
    private fun collectTargetSubstitutionFromMatchingTypes(
        initialType: ConeCangJieType,
        targetType: ConeCangJieType,
        freshTypeConstructors: Set<TypeConstructorMarker>,
        substitution: MutableMap<TypeConstructorMarker, ConeCangJieType>,
    ): Boolean {
        if (initialType is ConeTypeVariableType && initialType.typeConstructor in freshTypeConstructors) {
            val previous = substitution.putIfAbsent(initialType.typeConstructor, targetType)
            return previous == null || previous == targetType
        }

        if (initialType == targetType) return true

        if (initialType is ConeLookupTagBasedType && targetType is ConeLookupTagBasedType) {
            if (initialType.expandedClassIdOrPrimitiveClassId != targetType.expandedClassIdOrPrimitiveClassId) {
                return false
            }
            if (initialType.typeArguments.size != targetType.typeArguments.size) return false
            return initialType.typeArguments.zip(targetType.typeArguments).all { (initialArgument, targetArgument) ->
                collectTargetSubstitutionFromMatchingTypes(
                    initialArgument.type,
                    targetArgument.type,
                    freshTypeConstructors,
                    substitution,
                )
            }
        }

        return false
    }

    /**
     * enum constructor 既可以由外层 expected type 定型，也可以由
     * `Option<Int>.None` 这类 member access 的显式 enum owner 类型定型。
     * 后者是仓颉 enum sugar 的真实语义，不属于调用解析兜底。
     */
    private fun Candidate.enumConstructorTargetType(
        initialType: ConeCangJieType,
        ownerClassId: ClassId,
        resolutionMode: ResolutionMode,
    ): EnumConstructorTargetTypeResult {
        val expectedType = (resolutionMode as? ResolutionMode.WithExpectedType)
            ?.expectedType
            ?.fullyExpandedType()
            ?.takeIf { it.enumConstructorOwnerClassIdOrNull() == ownerClassId }
        if (expectedType != null) return EnumConstructorTargetTypeResult.Resolved(expectedType)

        val expectedInterfaceType = (resolutionMode as? ResolutionMode.WithExpectedType)
            ?.expectedType
            ?.fullyExpandedType()
            ?: return callInfo.explicitReceiver
                ?.coneTypeOrNull
                ?.fullyExpandedType()
                ?.takeIf { it.enumConstructorOwnerClassIdOrNull() == ownerClassId }
                ?.let(EnumConstructorTargetTypeResult::Resolved)
                ?: EnumConstructorTargetTypeResult.NotApplicable

        val initialOwnerType = initialType.fullyExpandedType() as? ConeRigidType
            ?: return EnumConstructorTargetTypeResult.NotApplicable
        val expectedRigidType = expectedInterfaceType as? ConeRigidType
            ?: return EnumConstructorTargetTypeResult.NotApplicable
        val expectedConstructor = with(session.typeContext) { expectedRigidType.typeConstructor() }
        val typeCheckerState = session.typeContext.newTypeCheckerState(
            errorTypesEqualToAnything = false,
            stubTypesEqualToAnything = false,
        )
        val correspondingSupertypes = AbstractTypeChecker.findCorrespondingSupertypes(
            typeCheckerState,
            initialOwnerType,
            expectedConstructor,
        )
        for (correspondingSupertype in correspondingSupertypes) {
            val current = correspondingSupertype as? ConeCangJieType ?: continue
            val substitution = linkedMapOf<TypeConstructorMarker, ConeCangJieType>()
            if (collectOwnerSubstitution(current, expectedInterfaceType, substitution)) {
                val targetType = CfirTypeSubstitutorByMap(substitution).substituteOrSelf(initialOwnerType)
                if (targetType.enumConstructorOwnerClassIdOrNull() == ownerClassId &&
                    !containsCandidateFreshVariable(targetType)
                ) {
                    return EnumConstructorTargetTypeResult.Resolved(targetType)
                }
            }
        }

        return if (correspondingSupertypes.isNotEmpty()) {
            EnumConstructorTargetTypeResult.UnableToInferExpectedOwner
        } else {
            EnumConstructorTargetTypeResult.NotApplicable
        }
    }

    /** expected type 到 enum owner 的反向推断结果。 */
    private sealed interface EnumConstructorTargetTypeResult {
        data class Resolved(val type: ConeCangJieType) : EnumConstructorTargetTypeResult
        data object UnableToInferExpectedOwner : EnumConstructorTargetTypeResult
        data object NotApplicable : EnumConstructorTargetTypeResult
    }

    /** 从已实例化的 enum supertype 与 expected interface 反向收集 owner fresh 变量替换。 */
    private fun collectOwnerSubstitution(
        actualType: ConeCangJieType,
        expectedType: ConeCangJieType,
        substitution: MutableMap<TypeConstructorMarker, ConeCangJieType>,
    ): Boolean {
        if (actualType is ConeTypeVariableType) {
            val previous = substitution.putIfAbsent(actualType.typeConstructor, expectedType)
            return previous == null || previous == expectedType
        }
        if (actualType is ConeLookupTagBasedType && expectedType is ConeLookupTagBasedType) {
            if (actualType.expandedClassIdOrPrimitiveClassId != expectedType.expandedClassIdOrPrimitiveClassId) {
                return false
            }
            if (actualType.typeArguments.size != expectedType.typeArguments.size) return false
            return actualType.typeArguments.zip(expectedType.typeArguments).all { (actualArgument, expectedArgument) ->
                collectOwnerSubstitution(actualArgument.type, expectedArgument.type, substitution)
            }
        }
        return actualType == expectedType
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
        // 官方 typealias 构造调用先经 alias target 替换生成构造器候选；
        // expected type 属于外层初始化/赋值检查，不能反向补全该候选并吞掉真实不匹配。
        if ((symbol as? CfirConstructorSymbol)?.typeAliasConstructorInfo != null &&
            typeArgumentMapping != TypeArgumentMapping.NoExplicitArguments
        ) {
            return false
        }
        if (symbol.takeIf { it.isBound }?.cfir !is CfirEnumConstructor) return true
        val initialEnumClassId = initialType.fullyExpandedType().enumConstructorOwnerClassIdOrNull() ?: return true
        val expectedEnumClassId = expectedType.fullyExpandedType().enumConstructorOwnerClassIdOrNull() ?: return false
        return initialEnumClassId == expectedEnumClassId
    }

    /**
     * 返回能够作为 enum constructor owner 的 class id。
     *
     * 普通 enum 使用自身 class id；标准库 `Option` 作为 enum sugar 的 class-like 载体在此被统一识别。
     */
    private fun ConeCangJieType.enumConstructorOwnerClassIdOrNull(): ClassId? = when (this) {
        is ConeEnumType -> classId
        is ConeClassLikeType -> classId.takeIf { it == StdlibClassIds.Option }
        else -> null
    }

    /**
     * 在类型确实代表指定 enum owner 时抽取 owner 的类型实参。
     *
     * 该函数同时支持 `ConeEnumType` 和用于 `Option` sugar 的 class-like 类型表示。
     */
    private fun ConeCangJieType.enumTypeArgumentsForClassId(classId: ClassId): List<ConeCangJieType>? = when (this) {
        is ConeEnumType -> typeArguments.map { it.type }.takeIf { this.classId == classId }
        is ConeClassLikeType -> typeArguments.map { it.type }.takeIf { this.classId == classId }
        else -> null
    }

    /**
     * 判断 synthetic fake function 是否应把期望类型作为 equality 约束参与完成。
     *
     * 赋值右侧保留初始化表达式自己的类型检查路径；非赋值场景下 fake function 需要 equality
     * 来避免把目标类型仅作为宽松 subtype 约束而丢失表达式精确类型。
     */
    private fun Candidate.isSyntheticFunctionCallThatShouldUseEqualityConstraint(
        expectedType: ConeCangJieType,
    ): Boolean {
        if (components.context.isInsideAssignmentRhs) return false
        val symbol = symbol as? CfirCallableSymbol ?: return false
        return symbol.origin == CfirDeclarationOrigin.Synthetic.FakeFunction && !expectedType.isUnitOrAny()
    }

    /**
     * 判断类型是否为不应驱动 synthetic fake function equality 约束的顶层类型。
     */
    private fun ConeCangJieType.isUnitOrAny(): Boolean {
        return with(session.typeContext) {
            this@isUnitOrAny.isUnit() || this@isUnitOrAny == ConeAnyType
        }
    }

    /**
     * 对指定候选执行约束系统完成。
     *
     * 调用方可复用已创建的 postponed analyzer；未提供时会基于当前 resolution context 创建新的 analyzer。
     * 完成期间发现的 postponed atom 会回调 analyzer 解析 lambda、callable reference 等上下文依赖表达式。
     */
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
        if (reanalyzeLocalLambdaInitializersAfterCompletion(candidate)) {
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
    }

    /**
     * 函数值调用会在 completion 后才拿到局部 lambda initializer 的真实参数类型。
     *
     * 首轮 completion 固定输入位点后，必须把 initializer body 恢复到首轮 body resolve 前的状态，
     * 按这些最终参数类型重算 body，并把 body 中产生的新约束合回当前候选系统；随后调用方会再次完成
     * 同一个候选，使返回类型与外层泛型调用看到的是重算后的结果。
     */
    private fun reanalyzeLocalLambdaInitializersAfterCompletion(candidate: Candidate): Boolean {
        if (candidate.localLambdaInitializerCompletions.isEmpty()) return false
        val substitutor = candidate.system.currentStorage()
            .buildCurrentSubstitutor(session.typeContext, emptyMap())
            .asCone()
        var reanalyzed = false
        for (completion in candidate.localLambdaInitializerCompletions) {
            val inferenceData = completion.data
            if (inferenceData.bodyReanalyzedAfterCallableValueCompletion) continue
            val applied = inferenceData.applyCompletionResult(
                completion.variable,
                substitutor,
                candidate.system.currentStorage(),
                restoreBodyResolveState = true,
            )
            if (!applied) continue

            val lambdaExpression = inferenceData.lambdaExpression
            val lambda = lambdaExpression.anonymousFunction
            val pclaInferenceSession = CfirPCLAInferenceSession(candidate, session.inferenceComponents)
            transformer.context.withAnonymousFunctionTowerDataContext(lambda.symbol) {
                transformer.context.withInferenceSession(pclaInferenceSession) {
                    transformer.declarationsTransformer.doTransformAnonymousFunctionBodyFromCallCompletion(
                        lambdaExpression,
                        null,
                    )
                }
                pclaInferenceSession.applyResultsToMainCandidate()
            }
            transformer.context.dropContextForAnonymousFunction(lambda)
            inferenceData.bodyReanalyzedAfterCallableValueCompletion = true
            reanalyzed = true
        }
        return reanalyzed
    }

    /**
     * 为 factory pattern 场景把 lambda 的返回类型替换为新的返回类型变量。
     *
     * 该类型变量作为 lambda 返回值和外层候选之间的桥接约束，允许 lambda body 的返回表达式继续反推工厂调用。
     */
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

    /**
     * 创建把完成结果写回 CFIR 树的 transformer。
     *
     * writer 会应用最终 substitutor，并同步处理 return type 计算、数据流分析结果、
     * 整数字面量/操作符近似以及 body resolve 上下文中的完成模式。
     */
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
            transformer.declarationsTransformer,
            mode,
        )
    }

    /**
     * 返回候选在当前替换状态下的完成后结果类型。
     */
    fun completedResultType(candidate: Candidate): ConeCangJieType = candidate.substitutedReturnType()

    /**
     * 为当前调用上下文创建 postponed argument 分析器。
     *
     * analyzer 将 lambda 分析器、推断组件和 call resolver 组合起来，供约束完成器在需要时解析延迟表达式。
     */
    fun createPostponedArgumentsAnalyzer(context: ResolutionContext): PostponedArgumentsAnalyzer {
        return PostponedArgumentsAnalyzer(
            context,
            LambdaAnalyzerImpl(),
            session.inferenceComponents,
            transformer.components.callResolver,
        )
    }

    /**
     * 约束系统完成期间使用的 lambda 分析器实现。
     *
     * 它负责按推断出的函数类型重写 lambda 参数、返回类型引用，解析 lambda body，
     * 并把返回表达式重新包装成可继续进入约束系统的 resolution atom。
     */
    private inner class LambdaAnalyzerImpl : LambdaAnalyzer {
        /**
         * 分析 lambda body 并返回可作为返回值约束来源的 atom 集合。
         *
         * 当 PCLA 会话启用时，lambda body 在独立的 PCLA inference session 中解析；
         * 否则解析期间产生的额外约束会被收集并返回给外层约束系统。
         */
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
            lambda.replaceMatchingParameterFunctionType(expectedFunctionType)
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
                ?: ResolutionMode.ContextIndependent
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
                    /*
                     * PCLA 会话在 lambda body 中使用 common constraint system 解析嵌套调用。
                     * 顶层无上下文 lambda 的参数 placeholder 约束也产生在这个系统中；
                     * body 完成后必须提交回当前候选，后续 completion 才能固定并写回函数类型。
                     */
                    pclaInferenceSession.applyResultsToMainCandidate()
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

        /**
         * 用推断出的输入类型重写 lambda 形参类型引用。
         *
         * 缺失的输入类型会写入 `ConeCannotInferValueParameterType`，其余类型会先按 lambda 输入位点近似，
         * 再根据原始形参是否显式写类型决定是创建新 resolved type ref 还是沿用原型 source/delegation。
         */
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
                val newTypeRef = if (parameter.returnTypeRef is CfirImplicitTypeRef) {
                    val source =
                        parameter.source?.fakeElement(CjFakeSourceElementKind.ImplicitReturnTypeOfLambdaValueParameter)
                    approximated.toResolvedTypeRef(source)
                } else {
                    val source = parameter.returnTypeRef.source
                    parameter.returnTypeRef.resolvedTypeFromPrototype(approximated, source)
                }
                parameter.replaceReturnTypeRef(newTypeRef)
            }
        }
    }

    /**
     * 将 lambda 输入类型近似为可安全写回形参声明的类型。
     *
     * 如果输入仍是不可报告为真实参数类型的 fresh type variable，则转成错误类型；
     * 否则使用类型近似器向上近似，避免把内部推断变量泄漏到 CFIR 形参类型引用。
     */
    private fun ConeCangJieType.approximateLambdaInputType(
        valueParameter: CfirValueParameterSymbol?,
        isRootLambdaForPCLASession: Boolean,
        containingCandidate: Candidate,
    ): ConeCangJieType {
        if (useErrorTypeInsteadOfTypeVariableForParameterType(isRootLambdaForPCLASession, containingCandidate)) {
            val diagnostic = valueParameter?.let {
                ConeCannotInferValueParameterType(
                    it,
                    isTopLevelLambda = containingCandidate.isSyntheticCallForTopLevelLambda(),
                )
            } ?: ConeCannotInferValueParameterType(null, "Cannot infer parameter type")
            return ConeErrorType(diagnostic)
        }
        if (containingCandidate.isSyntheticCallForTopLevelLambda() &&
            containsSyntheticTopLevelLambdaBoundaryVariable(containingCandidate)
        ) {
            return this
        }

        return session.typeApproximator.approximateToSuperType(
            this,
            TypeApproximatorConfiguration.IntermediateApproximationToSupertypeAfterCompletionInK2,
        ) ?: this
    }

    /**
     * 顶层无上下文 lambda 的参数类型可能由成员访问反推出复合边界，
     * 例如 `l.iterator()` 会得到 `Array<Tfresh>`。该 fresh variable 属于
     * synthetic accept 候选的约束系统，必须保留到局部 lambda initializer
     * 状态中，后续 `f([1, 2])` 才能继续约束元素类型。
     */
    private fun ConeCangJieType.containsSyntheticTopLevelLambdaBoundaryVariable(
        containingCandidate: Candidate,
    ): Boolean {
        val availableVariables = containingCandidate.system.currentStorage().allTypeVariables.keys
        if (availableVariables.isEmpty()) return false
        return contains { type ->
            type is org.cangnova.cangjie.cfir.types.ConeTypeVariableType &&
                    type.typeConstructor in availableVariables
        }
    }

    /**
     * 判断 lambda 形参类型中的 fresh type variable 是否应立即错误化。
     *
     * 普通无上下文 lambda 不应把内部推断变量暴露给形参类型；PCLA 根 lambda 和当前 PCLA 会话只错误化
     * 没有关联源码类型参数的变量。synthetic 顶层 lambda 按仓颉官方语义延后到 completion 阶段统一处理。
     */
    private fun ConeCangJieType.useErrorTypeInsteadOfTypeVariableForParameterType(
        isRootLambdaForPCLASession: Boolean,
        containingCandidate: Candidate,
    ): Boolean {
        if (this !is org.cangnova.cangjie.cfir.types.ConeTypeVariableType) return false

        /*
         * 仓颉官方 `SynLamExpr` 会为无上下文 lambda 参数创建 placeholder type variable，
         * 再由成员访问语法（MemSig/Mem2Decls）反推出接收者候选类型。synthetic 顶层
         * lambda 正是 CFIR 承载该语义的入口，因此这里不能沿用 Kotlin 对普通无上下文
         * lambda 的立即错误化策略；仍无法推断的变量会在后续 completion 阶段统一报错。
         */
        if (containingCandidate.isSyntheticCallForTopLevelLambda() && typeConstructor.originalTypeParameter == null) {
            return false
        }

        if (isRootLambdaForPCLASession || inferenceSession is CfirPCLAInferenceSession) {
            return false
        }
        return true
    }
}

/**
 * 判断候选函数是否属于 cast 期望类型特性可使用的简单泛型函数。
 *
 * 该函数要求无显式类型实参、唯一类型参数直接作为返回类型，并且该类型参数不出现在任何值参数类型中。
 */
private fun Candidate.isFunctionForExpectTypeFromCastFeature(): Boolean {
    if (typeArgumentMapping != TypeArgumentMapping.NoExplicitArguments) return false
    val cfir = symbol.cfir as? CfirFunction ?: return false
    return cfir.isFunctionForExpectTypeFromCastFeature()
}

/**
 * 判断函数声明是否符合 cast 期望类型驱动返回类型推断的结构条件。
 *
 * 条件成立时，调用完成可以把 cast 目标类型作为返回类型 subtype 约束加入，而不会反向污染入参类型。
 */
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

/**
 * 记录 class-like 类型解析 lookup。
 *
 * 当前实现保留接口入口，实际记录逻辑仍等待基本类型过滤策略收敛后启用。
 */
fun CfirLookupTrackerComponent.recordClassLikeLookup(classId: ClassId, source: CjSourceElement?, fileSource: CjSourceElement?) {
//TODO 排除基本类型
//    if ( classId !in StandardClassIds.allBuiltinTypes) {
//        val classFqName = classId.asSingleFqName()
//        recordLookup(classFqName.shortName().asString(), classFqName.parent().asString(), source, fileSource)
//    }
}

/**
 * 递归记录类型解析产生的 class-like lookup。
 *
 * 错误类型和缺失 source 的类型不会记录；普通类型会记录自身 class id，并继续递归记录所有类型实参。
 */
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

/**
 * 基于既有类型引用原型创建 resolved/error type ref。
 *
 * 新引用继承原型的 source 和 delegation 信息，并在目标类型为 `ConeErrorType` 时保留其诊断。
 */
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

/**
 * 将 Cone 类型直接包装为新的 resolved/error type ref。
 *
 * 该路径用于隐式 lambda 形参等没有可复用原型引用的位置。
 */
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

/**
 * 将当前类型断言为简单 Cone 类型。
 *
 * 调用点已经限定在类型参数包装判定路径，若结构不满足说明上游类型模型不符合该路径的不变量。
 */
private fun ConeCangJieType.unwrap(): ConeSimpleCangJieType = this as ConeSimpleCangJieType
