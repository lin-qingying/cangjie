package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.SessionHolder
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.isLambdaParameterTypeOmitted
import org.cangnova.cangjie.cfir.declarations.lambdaParameterShapeExpectedFunctionType
import org.cangnova.cangjie.cfir.diagnostic.AmbiguousArgumentType
import org.cangnova.cangjie.cfir.diagnostic.ArgumentTypeMismatch
import org.cangnova.cangjie.cfir.diagnostic.ConeAmbiguityError
import org.cangnova.cangjie.cfir.diagnostic.ConeConstraintSystemHasContradiction
import org.cangnova.cangjie.cfir.diagnostic.InapplicableWrongReceiver
import org.cangnova.cangjie.cfir.diagnostic.LambdaParameterCountMismatch
import org.cangnova.cangjie.cfir.diagnostic.LambdaParameterTypeMismatch
import org.cangnova.cangjie.cfir.diagnostic.UnsuccessfulCallableReferenceArgument
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.CfirDiagnosticHolder
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirArrayLiteral
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.CfirNamedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirSpawnExpression
import org.cangnova.cangjie.cfir.expressions.CfirTupleLiteral
import org.cangnova.cangjie.cfir.expressions.CfirWrappedExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildFunctionCallCopy
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedErrorReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.impl.CfirResolvedAppliedCallableReference
import org.cangnova.cangjie.cfir.references.builder.buildErrorNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildNamedReference
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.arrayLiteralTypeForSupertypeTarget
import org.cangnova.cangjie.cfir.resolve.withExpectedType
import org.cangnova.cangjie.cfir.resolve.CfirResolutionSnapshot
import org.cangnova.cangjie.cfir.resolve.calls.*
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallKind
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CheckerSink
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirNamedReferenceWithCandidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.addSubsystemFromAtom
import org.cangnova.cangjie.cfir.resolve.functionTypeForFunctionValueCandidate
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeArgumentConstraintPosition
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeExplicitTypeParameterConstraintPosition
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeReceiverConstraintPosition
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeRegularLambdaArgumentConstraintPosition
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeVariableForLambdaParameterType
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeVariableForLambdaReturnType
import org.cangnova.cangjie.cfir.semantics.AbstractCallCandidate
import org.cangnova.cangjie.cfir.semantics.AmbiguousClassifierTypeInCandidateSignature
import org.cangnova.cangjie.cfir.semantics.ErrorTypeInCandidateSignature
import org.cangnova.cangjie.cfir.semantics.ErrorTypeInArguments
import org.cangnova.cangjie.cfir.semantics.ResolutionDiagnostic
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterLookupTag
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeIdealLiteralType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConeLookupTagBasedType
import org.cangnova.cangjie.cfir.types.ConeTypeIntersector
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConePointerType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeUnreportedDuplicateDiagnostic
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.cfir.types.IdealTypeResolver
import org.cangnova.cangjie.cfir.types.arrayLiteralElementType
import org.cangnova.cangjie.cfir.types.asCone
import org.cangnova.cangjie.cfir.types.expandedClassIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.declaredUpperBoundConeTypeOrNull
import org.cangnova.cangjie.cfir.types.declaredUpperBoundRefsAfterTypeResolve
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.resolve.calls.inference.ConstraintSystemBuilder
import org.cangnova.cangjie.resolve.calls.inference.addEqualityConstraintIfCompatible
import org.cangnova.cangjie.resolve.calls.inference.addSubtypeConstraintIfCompatible
import org.cangnova.cangjie.resolve.calls.inference.isSubtypeConstraintCompatible
import org.cangnova.cangjie.resolve.calls.inference.runTransaction
import org.cangnova.cangjie.cfir.resolve.calls.applySpawnExpectedFutureType
import org.cangnova.cangjie.resolve.calls.inference.components.PostponedArgumentInputTypesResolver.Companion.TYPE_VARIABLE_NAME_FOR_LAMBDA_RETURN_TYPE
import org.cangnova.cangjie.resolve.calls.inference.components.PostponedArgumentInputTypesResolver.Companion.TYPE_VARIABLE_NAME_PREFIX_FOR_LAMBDA_PARAMETER_TYPE
import org.cangnova.cangjie.resolve.calls.inference.model.ArgumentConstraintPosition
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintKind
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintPosition
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * 实参检查处理器。
 *
 * 该对象负责把实参表达式类型与候选参数期望类型写入约束系统，
 * 并为 lambda、函数引用、数组字面量等需要延迟处理的实参创建 postponed atom。
 */
internal object ArgumentCheckingProcessor {
    /**
     * 单个实参检查过程需要的上下文。
     */
    private data class ArgumentContext(
        /**
         * 当前正在检查的调用候选。
         */
        val candidate: Candidate,
        /**
         * 当前候选的约束系统 builder。
         */
        val csBuilder: ConstraintSystemBuilder,
        /**
         * 当前实参位置上的期望类型。
         */
        val expectedType: ConeCangJieType?,
        /**
         * 诊断 sink；completion 阶段仅构造 atom 时可为空。
         */
        val sink: CheckerSink?,
        /**
         * 当前调用解析上下文。
         */
        val context: ResolutionContext,
        /**
         * 当前实参是否作为 receiver 检查。
         */
        val isReceiver: Boolean,
        /**
         * 当前 receiver 是否为 dispatch receiver。
         */
        val isDispatch: Boolean,
        /**
         * 当该实参来自 lambda 返回表达式时，对应的匿名函数。
         */
        val anonymousFunctionIfReturnExpression: CfirAnonymousFunction? = null,
    ) : SessionHolder {
        /**
         * 当前上下文所属会话。
         */
        override val session: CfirSession
            get() = context.session

        /**
         * 向 sink 报告诊断；sink 为空时忽略。
         */
        fun reportDiagnostic(diagnostic: ResolutionDiagnostic) {
            sink?.reportDiagnostic(diagnostic)
        }
    }

    /**
     * 外层实参检查对嵌套调用的解析结果。
     *
     * 嵌套调用在当前外层候选的 expected type 下重检失败时，失败属于内层调用；
     * 外层只能降低候选适用性，不能再把同一个失败重新分类为外层参数类型错误。
     */
    private data class NestedCallResolutionResult(
        val atom: ConeResolutionAtom,
        val failed: Boolean = false,
        val suppressOuterMismatch: Boolean = false,
    )

    /**
     * 解析并检查一个表达式实参。
     */
    fun resolveArgumentExpression(
        candidate: Candidate,
        atom: ConeResolutionAtom,
        expectedType: ConeCangJieType?,
        sink: CheckerSink,
        context: ResolutionContext,
        isReceiver: Boolean,
        isDispatch: Boolean,
        anonymousFunctionIfReturnExpression: CfirAnonymousFunction? = null,
    ) {
        ArgumentContext(
            candidate = candidate,
            csBuilder = candidate.system.getBuilder(),
            expectedType = expectedType,
            sink = sink,
            context = context,
            isReceiver = isReceiver,
            isDispatch = isDispatch,
            anonymousFunctionIfReturnExpression = anonymousFunctionIfReturnExpression,
        ).resolveArgumentExpression(atom)
    }

    /**
     * 直接使用已知实参类型进行适用性检查。
     */
    fun resolvePlainArgumentType(
        candidate: Candidate,
        atom: ConeResolutionAtom,
        argumentType: ConeCangJieType,
        expectedType: ConeCangJieType?,
        sink: CheckerSink,
        context: ResolutionContext,
        isReceiver: Boolean,
        isDispatch: Boolean,
        sourceForReceiver: CjSourceElement? = null,
    ) {
        ArgumentContext(
            candidate = candidate,
            csBuilder = candidate.system.getBuilder(),
            expectedType = expectedType,
            sink = sink,
            context = context,
            isReceiver = isReceiver,
            isDispatch = isDispatch,
        ).resolvePlainArgumentType(atom, argumentType, sourceForReceiver)
    }

    /**
     * completion 阶段按修订后的期望类型创建已解析 lambda atom。
     */
    fun createResolvedLambdaAtomDuringCompletion(
        candidate: Candidate,
        csBuilder: ConstraintSystemBuilder,
        atom: ConeResolutionAtomWithPostponedChild,
        expectedType: ConeCangJieType?,
        context: ResolutionContext,
        returnTypeVariable: ConeTypeVariableForLambdaReturnType?,
        anonymousFunctionIfReturnExpression: CfirAnonymousFunction? = null,
    ): ConeResolvedLambdaAtom {
        return ArgumentContext(
            candidate = candidate,
            csBuilder = csBuilder,
            expectedType = expectedType,
            sink = null,
            context = context,
            isReceiver = false,
            isDispatch = false,
            anonymousFunctionIfReturnExpression = anonymousFunctionIfReturnExpression,
        ).createResolvedLambdaAtom(atom, duringCompletion = true, returnTypeVariable = returnTypeVariable)
    }

    /**
     * 按 atom 类型分派实参解析。
     */
    private fun ArgumentContext.resolveArgumentExpression(atom: ConeResolutionAtom) {
        when (atom) {
            is ConeResolutionAtomWithPostponedChild -> when (atom.expression) {
                is CfirAnonymousFunctionExpression -> preprocessLambdaArgument(atom)
                is CfirNamedAccessExpression -> preprocessFunctionReferenceArgument(atom, atom.expression)
                else -> {
                    atom.useFallbackSubAtom()
                    val child = atom.subAtom
                    if (child != null) {
                        resolveArgumentExpression(child)
                    } else {
                        resolvePlainExpressionArgument(atom)
                    }
                }
            }

            is ConeResolutionAtomWithSingleChild -> {
                val child = atom.subAtom
                if (child != null) {
                    resolveArgumentExpression(child)
                } else {
                    resolvePlainExpressionArgument(atom)
                }
            }

            is ConeSimpleLeafResolutionAtom -> {
                if (!preprocessLateResolvedFunctionReferenceArgument(atom)) {
                    resolvePlainExpressionArgument(atom)
                }
            }

            is ConeAtomWithCandidate -> resolvePlainExpressionArgument(atom)

            is ConePostponedResolvedAtom -> Unit
        }
    }

    /**
     * 在期望类型已知时把函数引用实参转为 postponed callable reference atom。
     */
    private fun ArgumentContext.preprocessFunctionReferenceArgument(
        atom: ConeResolutionAtomWithPostponedChild,
        expression: CfirNamedAccessExpression,
    ) {
        val fallback = atom.fallbackSubAtom ?: run {
            atom.useFallbackSubAtom()
            resolvePlainExpressionArgument(atom)
            return
        }
        val targetExpectedType = expectedType
        if (targetExpectedType == null) {
            atom.useFallbackSubAtom()
            resolveArgumentExpression(fallback)
            return
        }

        val postponedAtom = ConeResolvedCallableReferenceAtom(
            expression = expression,
            expectedType = targetExpectedType,
        )
        atom.setPostponedSubAtom(postponedAtom)
        candidate.addPostponedAtom(postponedAtom)
    }

    /**
     * 将 argument atom 初建后才解析成函数重载集合的名字访问重新分类为 callable reference。
     *
     * 该路径与 [preprocessFunctionReferenceArgument] 共享同一个 postponed atom 模型；区别仅在于
     * 早期 atom 尚无 postponed-child 外壳，不能把后续解析永久锁死在普通错误类型上。
     */
    private fun ArgumentContext.preprocessLateResolvedFunctionReferenceArgument(
        atom: ConeSimpleLeafResolutionAtom,
    ): Boolean {
        val expression = atom.expression as? CfirNamedAccessExpression ?: return false
        val targetExpectedType = expectedType
        if (targetExpectedType == null) return false
        if (!expression.isFunctionReferenceCandidateSet()) return false

        val postponedAtom = ConeResolvedCallableReferenceAtom(
            expression = expression,
            expectedType = targetExpectedType,
        )
        candidate.addPostponedAtom(postponedAtom)
        return true
    }

    /**
     * 解析普通表达式实参类型。
     */
    private fun ArgumentContext.resolvePlainExpressionArgument(atom: ConeResolutionAtom) {
        // 嵌套调用作为实参且其自身解析已经失败时，内层根诊断由嵌套调用自身报告；
        // 外层不得再对同一实参表达式重复报告类型不匹配，否则会制造官方不存在的级联
        // ARGUMENT_TYPE_MISMATCH（见 arraysizedlit2）。注意不能以"在期望类型下重检失败"
        // 作为判据：内层调用合法但返回类型与期望不符时（如 depth_call），外层仍须如实
        // 报告 ARGUMENT_TYPE_MISMATCH。
        if (
            atom is ConeAtomWithCandidate &&
            atom.expression is CfirFunctionCall &&
            !atom.candidate.isSuccessful
        ) {
            return
        }
        val nestedCallResult = resolveNestedCallForExpectedType(atom)
        // 嵌套泛型调用推断失败：内层 UNABLE_TO_INFER_GENERIC_FUNC 已由 write back 节点渲染，
        // 外层直接跳过 subtype 检查，避免对同一实参追加 ARGUMENT_TYPE_MISMATCH 级联。
        if (nestedCallResult.suppressOuterMismatch) return
        val resolvedAtom = nestedCallResult.atom
        val targetTypedEnumType = expectedType?.let { expected ->
            resolvedAtom.expression.applyNoArgEnumConstructorTargetType(expected, session)
                ?: enumConstructorTargetTypeFromExpectedVariable(resolvedAtom.expression, expected)
        }
        val targetTypedSpawnType = expectedType?.let { expected ->
            (resolvedAtom.expression as? CfirSpawnExpression)?.applySpawnExpectedFutureType(expected, session)
        }
        val argumentType = targetTypedEnumType
            ?: targetTypedSpawnType
            ?: arrayLiteralTypeFromExpectedType(resolvedAtom.expression)
            ?: tupleLiteralTypeFromExpectedType(resolvedAtom.expression)
            ?: (resolvedAtom as? ConeAtomWithCandidate)?.candidate?.argumentExpressionType(context)
            ?: resolvedAtom.expression.coneTypeOrNull
            ?: return
        resolvePlainArgumentType(resolvedAtom, argumentType)
    }

    /**
     * 目标类型已知时，按当前 outer candidate 独立重解析嵌套调用。
     *
     * 初次无 expected type 的解析可能把嵌套调用落成歧义，或先选择一个仅适用于其它
     * outer candidate 的目标。官方会在每个外层候选的形参类型下独立重检嵌套调用；
     * 这里通过隔离解析生成候选局部 replacement，并把内层约束系统合入当前候选。
     */
    private fun ArgumentContext.resolveNestedCallForExpectedType(
        atom: ConeResolutionAtom,
    ): NestedCallResolutionResult {
        val functionCall = atom.expression as? CfirFunctionCall
            ?: return NestedCallResolutionResult(atom)
        val reference = functionCall.calleeReference as? CfirNamedReference
            ?: return NestedCallResolutionResult(atom)
        val currentCandidate = (reference as? CfirNamedReferenceWithCandidate)?.candidate
            ?: (atom as? ConeAtomWithCandidate)?.candidate
        val currentSymbol = currentCandidate?.symbol
            ?: (reference as? CfirResolvedNamedReference)?.resolvedSymbol
        val referenceDiagnostic = (reference as? CfirDiagnosticHolder)?.diagnostic
        val ambiguity = referenceDiagnostic as? ConeAmbiguityError
        val ambiguityCandidates = ambiguity
            ?.candidates
            ?.filterIsInstance<Candidate>()
        val isCallableAmbiguity = ambiguityCandidates != null &&
            ambiguityCandidates.isNotEmpty() &&
            ambiguityCandidates.size == ambiguity?.candidates?.size &&
            ambiguityCandidates.all { ambiguousCandidate ->
                ambiguousCandidate.symbol.takeIf { it.isBound }?.cfir.let {
                    it is CfirFunction || it is CfirEnumConstructor
                }
            }
        if (currentSymbol == null && !isCallableAmbiguity) return NestedCallResolutionResult(atom)
        val currentDeclaration = currentSymbol?.takeIf { it.isBound }?.cfir
        if (!isCallableAmbiguity &&
            currentSymbol != null &&
            currentDeclaration !is CfirEnumConstructor &&
            currentDeclaration !is CfirFunction
        ) {
            return NestedCallResolutionResult(atom)
        }
        val targetExpectedType = expectedType ?: return NestedCallResolutionResult(atom)
        val expandedExpectedType = targetExpectedType.fullyExpandedType(session)
        val expectedOwnerClassId = expandedExpectedType.classIdOrPrimitiveClassId
        if (currentSymbol is CfirEnumConstructorSymbol && expectedOwnerClassId != null && !isCallableAmbiguity) {
            val currentReturnType = currentCandidate?.substitutedReturnType()
                ?: (reference as? CfirResolvedAppliedCallableReference)?.substitutedReturnType
                ?: functionCall.coneTypeOrNull
                ?: return NestedCallResolutionResult(atom)
            val currentOwnerClassId = (currentSymbol as? CfirEnumConstructorSymbol)
                ?.let(session.cfirProvider::getContainingClass)
                ?.classId
                ?: return NestedCallResolutionResult(atom)
            if (
                currentOwnerClassId == expectedOwnerClassId &&
                AbstractTypeChecker.equalTypes(
                    session.typeContext,
                    currentReturnType.fullyExpandedType(session),
                    expandedExpectedType,
                )
            ) return NestedCallResolutionResult(atom)
        }
        if (currentDeclaration is CfirFunction && !isCallableAmbiguity) {
            val currentReturnType = currentCandidate?.argumentExpressionType(context)
                ?: (reference as? CfirResolvedAppliedCallableReference)?.substitutedReturnType
                ?: functionCall.coneTypeOrNull
                ?: return NestedCallResolutionResult(atom)
            /*
             * 无歧义的嵌套泛型调用已经拥有唯一候选时，其 fresh 返回变量就是外层 expected type
             * 应继续约束的变量。若在这里重新复制调用并创建候选，会产生另一套 fresh variables：
             * expected type 进入新系统，而 outer argument atom 仍持有旧系统，FULL completion 最终会
             * 把旧变量误报为无法推断。保留原 atom 后，统一实参检查会先合入同一候选子系统，再把
             * expected-type 约束写到该 fresh variable 上。
             */
            if (
                atom is ConeAtomWithCandidate &&
                currentCandidate === atom.candidate &&
                currentCandidate.isSuccessful &&
                currentCandidate.ownsNotFixedTypeVariableIn(currentReturnType)
            ) {
                return NestedCallResolutionResult(atom)
            }
            if (
                AbstractTypeChecker.isSubtypeOf(
                    session.typeContext,
                    currentReturnType.fullyExpandedType(session),
                    expandedExpectedType,
                ) == true
            ) return NestedCallResolutionResult(atom)
        }

        val isolatedCall = buildFunctionCallCopy(functionCall) {
            calleeReference = buildNamedReference {
                source = reference.source
                name = reference.name
            }
            // 上下文敏感重解析的目标是用期望类型重新推断，因此必须丢弃上一轮推断写回、
            // 没有源码位置的合成类型实参（source == null）；否则它们会被当成“显式类型实参”，
            // 使 CfirCheckExpectedReturnTypeBeforeArguments 跳过期望类型注入，导致嵌套泛型调用
            // 无法在返回类型下产生推断失败（如 returnTypeInferenceMismatch 的 `produce(true)`）。
            typeArguments.retainAll { it.source != null }
        }
        val precollectedDiscoveries = context.bodyResolveComponents.callResolver
            .expectedTypeRefinementDiscovery(functionCall)
        val resolutionSnapshot = CfirResolutionSnapshot.capture(functionCall)
        val resolvedProbe = try {
            context.bodyResolveContext.dataFlowAnalyzerContext.withIsolatedContext {
                // 名字查找结果不依赖 outer expected type。已有 callable ambiguity 能被目标返回类型
                // 唯一规约时，只从 discovery 字段创建 fresh candidate 并重跑正常 stages；泛型或
                // 仍有多个 survivor 的情况继续走完整 tower resolver。
                if (precollectedDiscoveries != null) {
                    context.bodyResolveComponents.callResolver.resolveCallFromPrecollectedCandidates(
                        functionCall = isolatedCall,
                        resolutionMode = withExpectedType(targetExpectedType),
                        discoveries = precollectedDiscoveries,
                    )?.let { return@withIsolatedContext it }
                }
                val resolvedCall = context.bodyResolveComponents.callResolver.resolveCallAndSelectCandidate(
                    isolatedCall,
                    withExpectedType(targetExpectedType),
                )
                val resolvedReference = resolvedCall.calleeReference as? CfirNamedReferenceWithCandidate
                    ?: return@withIsolatedContext null
                resolvedReference.candidate to resolvedCall
            }
        } finally {
            // buildFunctionCallCopy 会共享内层实参节点；下一 outer candidate 检查前必须恢复共享状态。
            resolutionSnapshot.restore()
        } ?: run {
            return NestedCallResolutionResult(atom)
        }
        val (resolvedCandidate, resolvedCall) = resolvedProbe
        if (!resolvedCandidate.isSuccessful) {
            /*
             * 嵌套泛型调用在期望类型下无法推断类型实参（约束系统矛盾/信息不足）时，
             * 官方根诊断是锚定被调函数标识符的 UNABLE_TO_INFER_GENERIC_FUNC，而不是把
             * 整棵嵌套调用降级为外层 ARGUMENT_TYPE_MISMATCH 级联。把隔离解析的调用连同
             * 其失败诊断写回真实调用节点，让共享诊断映射层按普通泛型调用失败渲染；
             * 其余嵌套失败保持既有 ErrorTypeInArguments 语义。
             */
            if (resolvedCall.isNestedGenericInferenceFailure(resolvedCandidate)) {
                candidate.setUpdatedArgumentFromContextSensitiveResolution(functionCall, resolvedCall)
                /*
                 * 内层泛型调用推断失败（约束矛盾/信息不足）的根诊断 UNABLE_TO_INFER_GENERIC_FUNC
                 * 已由 write back 的调用节点在共享映射层渲染，且锚定内层 callee。外层不得再对同一
                 * 实参报 ARGUMENT_TYPE_MISMATCH，否则会制造官方不存在的级联（如 returnTypeInferenceMismatch
                 * 的 expectInt(produce(true))）。这里把「该外层参数已完成诊断、需要抑制 mismatch」的信号
                 * 传回 resolvePlainExpressionArgument，使其跳过 subtype 检查。
                 */
                return NestedCallResolutionResult(atom, failed = true, suppressOuterMismatch = true)
            }
            candidate.addDiagnostic(ErrorTypeInArguments)
            return NestedCallResolutionResult(atom, failed = true)
        }
        val resolvedDeclaration = resolvedCandidate.symbol.takeIf { it.isBound }?.cfir
        if (resolvedDeclaration !is CfirEnumConstructor && resolvedDeclaration !is CfirFunction) {
            return NestedCallResolutionResult(atom)
        }
        if (resolvedDeclaration is CfirEnumConstructor && expectedOwnerClassId != null) {
            val resolvedOwnerClassId = (resolvedCandidate.symbol as? CfirEnumConstructorSymbol)
                ?.let(session.cfirProvider::getContainingClass)
                ?.classId
                ?: return NestedCallResolutionResult(atom)
            if (resolvedOwnerClassId != expectedOwnerClassId) return NestedCallResolutionResult(atom)
        }
        // 此处不能要求候选返回类型已经等于 expected type：泛型 enum constructor 的 owner
        // fresh variable 要在下面把 inner subsystem 合入 outer candidate 后，才由实参约束共同固定。
        // 普通重载的 expected-return 筛选由 call resolver 负责，最终兼容性由统一实参约束判断。
        candidate.setUpdatedArgumentFromContextSensitiveResolution(
            functionCall,
            resolvedCall,
        )
        return NestedCallResolutionResult(ConeAtomWithCandidate(resolvedCall, resolvedCandidate))
    }

    /**
     * 判断隔离重解析失败的嵌套调用是否属于「无显式类型实参的泛型调用在期望类型下无法推断
     * 类型实参」。
     *
     * 官方对这类调用只报锚定被调函数标识符的 `UNABLE_TO_INFER_GENERIC_FUNC`
     * （sema_unable_to_infer_generic_func），而不是把整棵嵌套调用降级为外层
     * `ARGUMENT_TYPE_MISMATCH` 级联。特征是：被调对象是未提供显式类型实参的泛型
     * 函数/构造器，且其失败根诊断是约束系统矛盾。
     */
    private fun CfirFunctionCall.isNestedGenericInferenceFailure(candidate: Candidate): Boolean {
        if (candidate.symbol !is CfirCallableSymbol<*>) return false
        if (candidate.symbol is CfirEnumConstructorSymbol) return false
        if (candidate.callInfo.hasExplicitTypeArguments) return false
        val declaration = candidate.symbol.takeIf { it.isBound }?.cfir
        if (declaration !is CfirFunction && declaration !is CfirConstructor) return false
        if (declaration.typeParameters.isEmpty()) return false
        val diagnostic = (calleeReference as? CfirDiagnosticHolder)?.diagnostic
        return diagnostic is ConeConstraintSystemHasContradiction
    }

    /** 判断类型树中的 fresh variable 是否属于当前嵌套调用候选自己的约束系统。 */
    private fun Candidate.ownsNotFixedTypeVariableIn(type: ConeCangJieType): Boolean {
        val notFixedTypeVariables = system.asReadOnlyStorage().notFixedTypeVariables

        fun containsOwnedVariable(current: ConeCangJieType): Boolean = when (current) {
            is ConeTypeVariableType -> current.typeConstructor in notFixedTypeVariables
            is ConeLookupTagBasedType -> current.typeArguments.any { containsOwnedVariable(it.type) }
            is ConeFunctionType -> current.parameterTypes.any(::containsOwnedVariable) ||
                    containsOwnedVariable(current.returnType)
            is ConeTupleType -> current.elementTypes.any(::containsOwnedVariable)
            is ConeVArrayType -> containsOwnedVariable(current.elementType)
            else -> false
        }

        return containsOwnedVariable(type)
    }

    /** 取得候选作为实参表达式时的类型；函数值引用使用完整函数类型而不是返回值类型。 */
    private fun Candidate.argumentExpressionType(context: ResolutionContext): ConeCangJieType {
        val resolvedCallSite = callInfo.callSite as? CfirNamedAccessExpression
        val completedExpressionType = resolvedCallSite
            ?.takeIf { it.calleeReference !is CfirNamedReferenceWithCandidate }
            ?.coneTypeOrNull
        if (completedExpressionType != null) return completedExpressionType

        val function = symbol.takeIf { it.isBound }?.cfir as? CfirFunction
        if (function != null && callInfo.callKind == CallKind.NamedValueAccess) {
            return context.bodyResolveComponents.functionTypeForFunctionValueCandidate(this, function)
        }
        return substitutedReturnType()
    }

    /**
     * expected type 是当前推断变量时，从该变量已经收集到的 owner 形状约束中反推
     * 无参 enum constructor 的目标类型。
     */
    private fun ArgumentContext.enumConstructorTargetTypeFromExpectedVariable(
        expression: CfirExpression,
        expectedType: ConeCangJieType,
    ): ConeCangJieType? {
        val expectedVariableType = expectedType as? ConeTypeVariableType ?: return null
        val constraints = csBuilder.currentStorage()
            .notFixedTypeVariables[expectedVariableType.typeConstructor]
            ?.constraints
            ?: return null

        var targetType: ConeCangJieType? = null
        for (constraint in constraints) {
            if (!constraint.kind.impliesLower()) continue
            val constraintType = constraint.type as? ConeCangJieType ?: continue
            val enumTargetType = expression.noArgEnumConstructorTargetType(constraintType, session) ?: continue
            val previousTargetType = targetType
            if (previousTargetType == null) {
                targetType = enumTargetType
                continue
            }
            if (!AbstractTypeChecker.equalTypes(session.typeContext, previousTargetType, enumTargetType)) {
                return null
            }
        }

        return targetType?.let { expression.applyNoArgEnumConstructorTargetType(it, session) }
    }

    /**
     * 当期望类型可确定数组字面量元素类型时，为数组字面量补齐整体类型。
     */
    private fun ArgumentContext.arrayLiteralTypeFromExpectedType(expression: CfirExpression): ConeCangJieType? {
        val arrayLiteral = expression as? CfirArrayLiteral ?: return null
        val expandedExpectedType = expectedType?.fullyExpandedType(session) ?: return null
        /*
         * 目标类型是 `Array<E>` 的超类型时（如 `ArrayList<T>` 构造器形参 `Collection<T>`），
         * 官方仍按元素视角 `E` 定形字面量。这里合成的必须是 `Array<E>` 本身，而不是该超类型。
         * 含当前候选未固定变量的结果继续交给普通约束路径，避免把推断变量写进 CFIR 节点。
         */
        val targetArrayType = when {
            expandedExpectedType.arrayLiteralElementType != null -> expandedExpectedType
            else -> expandedExpectedType.arrayLiteralTypeForSupertypeTarget(session)
                ?.takeUnless { typeContainsCurrentInferenceVariable(it) }
        } ?: return null
        val expectedElementType = targetArrayType.arrayLiteralElementType ?: return null
        if (targetArrayType is ConeVArrayType && targetArrayType.size != arrayLiteral.elements.size.toLong()) {
            return null
        }

        val elementsCompatible = arrayLiteral.elements.all { element ->
            val elementType = element.coneTypeOrNull?.let { IdealTypeResolver.resolveIfIdeal(it, expectedElementType) }
                ?: return@all false
            elementType is ConeErrorType ||
                    expectedElementType is ConeErrorType ||
                    AbstractTypeChecker.equalTypes(session.typeContext, elementType, expectedElementType) ||
                    AbstractTypeChecker.isSubtypeOf(session.typeContext, elementType, expectedElementType)
        }
        if (!elementsCompatible) return null

        arrayLiteral.replaceConeTypeOrNull(targetArrayType)
        return targetArrayType
    }

    /**
     * 当期望类型是 tuple 时，允许元素级 expected type 修复 `None` 等需要上下文的子表达式。
     */
    private fun ArgumentContext.tupleLiteralTypeFromExpectedType(expression: CfirExpression): ConeCangJieType? {
        val tupleLiteral = expression as? CfirTupleLiteral ?: return null
        val expectedTupleType = expectedType?.fullyExpandedType(session) as? ConeTupleType ?: return null
        if (expectedTupleType.elementTypes.size != tupleLiteral.elements.size) return null

        val elementsCompatible = tupleLiteral.elements.zip(expectedTupleType.elementTypes).all { (element, expectedElementType) ->
            val elementType = typeForExpectedTupleElement(element, expectedElementType)
                ?: return@all false
            elementType is ConeErrorType ||
                    expectedElementType is ConeErrorType ||
                    AbstractTypeChecker.equalTypes(session.typeContext, elementType, expectedElementType) ||
                    AbstractTypeChecker.isSubtypeOf(session.typeContext, elementType, expectedElementType)
        }
        if (!elementsCompatible) return null

        tupleLiteral.replaceConeTypeOrNull(expectedTupleType)
        return expectedTupleType
    }

    /**
     * tuple 参数的元素级目标类型传播。
     *
     * 这里复用无参 enum constructor 的目标类型语义，使 `None` 在参数 tuple 内也能从
     * 形参的 `Option<T>` 元素类型定型，而不是保留无上下文解析阶段的泛型推断错误。
     */
    private fun ArgumentContext.typeForExpectedTupleElement(
        element: CfirExpression,
        expectedElementType: ConeCangJieType,
    ): ConeCangJieType? {
        if (element is CfirWrappedExpression) {
            val innerType = typeForExpectedTupleElement(element.expression, expectedElementType)
            if (innerType != null) {
                element.replaceConeTypeOrNull(innerType)
                return innerType
            }
        }

        val directType = element.coneTypeOrNull?.let { IdealTypeResolver.resolveIfIdeal(it, expectedElementType) }
        val targetTypedEnumType = element.applyNoArgEnumConstructorTargetType(expectedElementType, session)
        return targetTypedEnumType ?: directType
    }

    /**
     * 创建实参约束位置；lambda 返回表达式使用专门的位置对象。
     */
    private fun  ArgumentContext.createArgumentConstraintPosition(atom: ConeResolutionAtom): ArgumentConstraintPosition<*> {
        return when (val containingLambda = anonymousFunctionIfReturnExpression) {
            null -> ConeArgumentConstraintPosition(atom.expression)
            else -> ConeRegularLambdaArgumentConstraintPosition(containingLambda, atom.expression)
        }
    }

    /**
     * 将普通实参类型写入 subtype 约束并报告适用性错误。
     */
    private fun ArgumentContext.resolvePlainArgumentType(
        atom: ConeResolutionAtom,
        argumentType: ConeCangJieType,
        sourceForReceiver: CjSourceElement? = null,
    ) {
        val expression = atom.expression
        val position = when {
            isReceiver -> ConeReceiverConstraintPosition(expression, sourceForReceiver)
            else -> createArgumentConstraintPosition(atom)
        }
        val preparedType = prepareArgumentType(argumentType, context.session)
        checkApplicabilityForArgumentType(atom, preparedType, position)
    }

    /**
     * 检查实参类型是否适用于期望类型。
     */
    private fun ArgumentContext.checkApplicabilityForArgumentType(
        atom: ConeResolutionAtom,
        argumentTypeBeforeCapturing: ConeCangJieType,
        position: ConstraintPosition,
    ) {
        if (expectedType == null) return

        if (atom is ConeAtomWithCandidate) {
            candidate.system.addSubsystemFromAtom(atom)
        }


        /*
         * current substitutor 只包含 fixedTypeVariables，不会替换仍待求解的 fresh variables。
         * 因此这里必须无条件正规化：同一类型树可以同时包含 fixed 与 not-fixed 变量，按整棵树
         * 跳过替换会让已经固定的变量重新进入后续约束注入。
         */
        val argumentTypeAfterCurrentSubstitution = csBuilder.buildCurrentSubstitutor()
            .asCone()
            .substituteOrNull(argumentTypeBeforeCapturing)
            ?: argumentTypeBeforeCapturing
        val argumentType = when {
            /*
             * 当前候选系统里的 fresh type variable 必须继续作为约束目标。
             * 若在这里把 `id<T>(1)` 的返回 `T` 还原成声明类型参数或上界，外层
             * `foo(Int64)` 就无法把 expected type 反向写入内层调用系统。
             */
            typeContainsCurrentInferenceVariable(argumentTypeAfterCurrentSubstitution) -> argumentTypeAfterCurrentSubstitution
            else -> substituteTypeParameterUpperBoundIfNeeded(argumentTypeAfterCurrentSubstitution, expectedType, session)
        }
        val expression = atom.expression

        /*
         * Error type 是内层解析失败的结构化载体，不是“与任意类型成功兼容”的普通实参。
         * 必须在 subtype compatibility 的错误恢复规则吞掉它之前降低外层候选适用性；
         * 实参错误和普通声明签名错误只保留内层根诊断；只有重声明 classifier 造成的
         * 参数签名歧义会通过专用 subtype 保留官方 no-match 级联。
         */
        if (expectedType is ConeErrorType) {
            val signatureDiagnostic = expectedType.diagnostic.unwrapUnreportedDuplicate()
            reportDiagnostic(
                if (signatureDiagnostic is ConeAmbiguityError && signatureDiagnostic.typeUseSource != null) {
                    AmbiguousClassifierTypeInCandidateSignature
                } else {
                    ErrorTypeInCandidateSignature
                }
            )
            return
        }
        if (argumentType is ConeErrorType) {
            reportDiagnostic(ErrorTypeInArguments)
            return
        }

        /*
         * `CPointer<T>(CPointer<U>)` 是仓颉的内建指针转换，而不是普通 invariant
         * 泛型调用。官方 `PointerExpr` 允许任意 pointee 类型之间的转换；该规则
         * 只适用于 synthetic pointer-conversion candidate，不能放宽用户泛型的
         * 同构类型实参检查。
         */
        if (
            candidate.isBuiltinPointerConstructorCandidate() &&
            (argumentType is ConePointerType ||
                argumentType is ConeFunctionType && argumentType.isCFunc) &&
            expectedType.fullyExpandedType(session) is ConePointerType
        ) {
            return
        }

        fun subtypeError(actualExpectedType: ConeCangJieType): ResolutionDiagnostic {
            fun tryGetConeTypeThatCompatibleWithKtType(type: ConeCangJieType): ConeCangJieType {
                if (type is ConeTypeVariableType) {
                    val lookupTag = type.typeConstructor

                    val constraints = csBuilder.currentStorage().notFixedTypeVariables[lookupTag]?.constraints
                    val constraintTypes = constraints?.mapNotNull { it.type as? ConeCangJieType }
                    if (!constraintTypes.isNullOrEmpty()) {
                        return ConeTypeIntersector.intersectTypes(session.typeContext, constraintTypes)
                    }

                    val originalTypeParameter = lookupTag.originalTypeParameter as? ConeTypeParameterLookupTag
                    if (originalTypeParameter != null) {
                        return ConeTypeParameterTypeImpl(originalTypeParameter , type.attributes)
                    }
                } else if (type is ConeIdealLiteralType) {
                    return type.defaultType
                }

                return type
            }

            val preparedExpectedType = tryGetConeTypeThatCompatibleWithKtType(actualExpectedType)
            val preparedActualType = tryGetConeTypeThatCompatibleWithKtType(argumentType)
            return ArgumentTypeMismatch(
                preparedExpectedType,
                preparedActualType,
                expression,
                false,
                anonymousFunctionIfReturnExpression,
                csBuilder.hasContradiction,
            )
        }

        val sameClassifierConstraintsAdded = addSameClassifierTypeArgumentConstraints(argumentType, expectedType, position)
        if (sameClassifierConstraintsAdded) return
        if (candidate.isEnumConstructorPayloadInference() &&
            addIdealLiteralEqualityConstraintForCurrentInferenceVariable(argumentType, expectedType, position)
        ) return
        if (addNoArgEnumConstructorShapeConstraintForCurrentInferenceVariable(expression, argumentType, expectedType, position)) return
        if (addLocalLambdaParameterShapeConstraint(argumentType, expectedType, position)) return
// IdealInt/IdealFloat 在具体形参类型下按候选局部目标类型参与适用性检查。
        // 嵌套调用的共享字面量节点可能已被先前候选具体化，因此从不可变 literal kind 重建
        // candidate-local ideal 类型；这里只替换约束输入，最终 winner 仍由 completion writer 写回类型。
        // 重建只适用于数值字面量：非数值实参（class-like/函数/字符串等）即使落在字面量表达式
        // 节点上（如测试夹具构造的类型化表达式）也必须保留原类型参与检查。
        val candidateLocalArgumentType = when (argumentType) {
            is ConePrimitiveType -> when (argumentType.kind) {
                PrimitiveTypeKind.IDEAL_INT -> ConePrimitiveType.IDEAL_INT
                PrimitiveTypeKind.IDEAL_FLOAT -> ConePrimitiveType.IDEAL_FLOAT
                else -> when ((expression as? CfirLiteralExpression)?.kind) {
                    CfirLiteralKind.INT -> ConePrimitiveType.IDEAL_INT
                    CfirLiteralKind.FLOAT -> ConePrimitiveType.IDEAL_FLOAT
                    else -> argumentType
                }
            }
            else -> argumentType
        }
        val argumentTypeForSubtypeCheck = if (csBuilder.isProperType(expectedType)) {
            IdealTypeResolver.resolveIfIdeal(candidateLocalArgumentType, expectedType)
        } else {
            candidateLocalArgumentType
        }
        // 官方 LocalTypeArgumentSynthesis 在求解类型参数后会把 IdealInt/IdealFloat
        // 归一化为默认具体类型。当前候选 fresh variable 若先接收原始 ideal lower bound，
        // outer expected type 会继续把同一泛型调用特化成多个数值类型并制造误歧义。
        if (addIdealLiteralConstraintForCurrentInferenceVariable(
                candidateLocalArgumentType,
                expectedType,
                position,
            )
        ) return
        val added = if (csBuilder.isProperType(expectedType)) {
            csBuilder.addSubtypeConstraintIfCompatible(argumentTypeForSubtypeCheck, expectedType, position)
        } else {
            csBuilder.addSubtypeConstraint(argumentTypeForSubtypeCheck, expectedType, position)
            true
        }
        if (added) return
        if (argumentType.isIdealTypeForTypeParameterExpectedType(expectedType)) return
        if (isReceiver) {
            csBuilder.addSubtypeConstraint(argumentType, expectedType, position)
            reportDiagnostic(InapplicableWrongReceiver(expectedType, argumentType))
            return
        }
        if (expression.isFunctionDeclarationReferenceArgument()) {
            val expandedArgumentType = argumentType.fullyExpandedType(session)
            val expandedExpectedType = expectedType.fullyExpandedType(session)
            if (expandedArgumentType is ConeFunctionType && expandedExpectedType is ConeFunctionType) {
                reportDiagnostic(UnsuccessfulCallableReferenceArgument(expression))
                return
            }
        }
        reportDiagnostic(subtypeError(expectedType))
    }

    /** 判断当前候选是否是带一个指针实参的内建转换构造器。 */
    private fun Candidate.isBuiltinPointerConstructorCandidate(): Boolean {
        val declaration = symbol.takeIf { it.isBound }?.cfir as? CfirFunction ?: return false
        return declaration.origin == CfirDeclarationOrigin.Synthetic.BuiltinPointerConstructor &&
                declaration.valueParameters.size == 1
    }

    /** 还原错误恢复传播时包装的原始 Cone 诊断。 */
    private tailrec fun ConeDiagnostic.unwrapUnreportedDuplicate(): ConeDiagnostic =
        if (this is ConeUnreportedDuplicateDiagnostic) original.unwrapUnreportedDuplicate() else this

    /**
     * 同构泛型实参约束下沉。
     *
     * 官方 LocalTypeArgumentSynthesis 会从 `Array<Int64>` 对 `Array<T>` 这类同构类型中继续
     * 提取元素级约束，用于完成函数/owner 泛型参数。底层 subtype 检查只回答整体是否可适用，
     * 不保证把每个类型实参都登记到当前候选约束系统，因此这里在实参检查共享层补齐该输入。
     */
    private fun ArgumentContext.addSameClassifierTypeArgumentConstraints(
        argumentType: ConeCangJieType,
        expectedType: ConeCangJieType,
        position: ConstraintPosition,
    ): Boolean {
        var hasAcceptedConstraint = false
        return csBuilder.runTransaction {
            hasAcceptedConstraint = addSameClassifierTypeArgumentConstraintsInTransaction(
                argumentType,
                expectedType,
                position,
            )
            hasAcceptedConstraint && !hasContradiction
        } && hasAcceptedConstraint
    }

    /**
     * 在同一个事务中递归分解 invariant 类型实参。
     *
     * 若分解过程中发现固定类型实参不相等或新增约束不兼容，外层事务会回滚本次
     * 同构下沉留下的全部部分约束，再交给普通 subtype 路径产出诊断。
     */
    private fun ArgumentContext.addSameClassifierTypeArgumentConstraintsInTransaction(
        argumentType: ConeCangJieType,
        expectedType: ConeCangJieType,
        position: ConstraintPosition,
    ): Boolean {
        /*
         * completion 会把已固定变量从 notFixedTypeVariables 移入 fixedTypeVariables；后续
         * 同构分解必须先应用这部分替换。固定变量已经是普通类型，不能再次作为约束左值
         * 交给 ConstraintInjector，否则会破坏“fixed variable 不再接收新约束”的不变量。
         */
        val currentSubstitutor = csBuilder.buildCurrentSubstitutor().asCone()
        val substitutedArgumentType = currentSubstitutor.substituteOrNull(argumentType) ?: argumentType
        val substitutedExpectedType = currentSubstitutor.substituteOrNull(expectedType) ?: expectedType
        val actualFunction = substitutedArgumentType.fullyExpandedType(session) as? ConeFunctionType
        val expectedFunction = substitutedExpectedType.fullyExpandedType(session) as? ConeFunctionType
        if (actualFunction != null || expectedFunction != null) {
            if (actualFunction == null || expectedFunction == null) return false
            return addFunctionTypeArgumentConstraintsInTransaction(
                actualFunction,
                expectedFunction,
                position,
            )
        }
        val actualClassifier = substitutedArgumentType.fullyExpandedType(session) as? ConeLookupTagBasedType ?: return false
        val expectedClassifier = substitutedExpectedType.fullyExpandedType(session) as? ConeLookupTagBasedType ?: return false
        if (actualClassifier.expandedClassIdOrPrimitiveClassId != expectedClassifier.expandedClassIdOrPrimitiveClassId) {
            return false
        }
        if (actualClassifier.typeArguments.size != expectedClassifier.typeArguments.size) return false

        var hasAcceptedConstraint = false
        for ((actualArgument, expectedArgument) in actualClassifier.typeArguments.zip(expectedClassifier.typeArguments)) {
            val actualArgumentType = actualArgument.type
            val expectedArgumentType = expectedArgument.type
            val actualContainsInferenceVariable = typeContainsCurrentInferenceVariable(actualArgumentType)
            val expectedContainsInferenceVariable = typeContainsCurrentInferenceVariable(expectedArgumentType)
            if (!actualContainsInferenceVariable && !expectedContainsInferenceVariable) {
                if (!isInvariantArgumentCompatible(actualArgumentType, expectedArgumentType, session)) return false
                continue
            }

            val decomposedNested = addSameClassifierTypeArgumentConstraintsInTransaction(
                actualArgumentType,
                expectedArgumentType,
                position,
            )
            if (decomposedNested) {
                hasAcceptedConstraint = true
                continue
            }

            val (currentVariableType, otherType) = when {
                isCurrentInferenceVariableType(actualArgumentType) -> actualArgumentType to expectedArgumentType
                isCurrentInferenceVariableType(expectedArgumentType) -> expectedArgumentType to actualArgumentType
                else -> return false
            }
            if (!csBuilder.addEqualityConstraintIfCompatible(currentVariableType, otherType, position)) {
                return false
            }
            hasAcceptedConstraint = true
        }
        return hasAcceptedConstraint
    }

    /**
     * 按官方 `LocalTypeArgumentSynthesis::UnifyFuncTy` 的函数类型统一规则下沉约束。
     *
     * 普通 subtype 检查对函数参数使用逆变，因此 `(Float32) -> Float32` 作为
     * `(T1) -> T2` 实参时只会得到 `T1 <: Float32`，不能为 flow composition 提供
     * 可完成的 `T1 == Float32`。仓颉的泛型实参合成会逐个统一函数参数和返回值；
     * 这里仅在函数类型树确实包含当前候选的推断变量时采用同一精确统一，并继续
     * 复用 nominal 类型的同构分解，避免改变普通非泛型函数 subtype 语义。
     */
    private fun ArgumentContext.addFunctionTypeArgumentConstraintsInTransaction(
        actualType: ConeFunctionType,
        expectedType: ConeFunctionType,
        position: ConstraintPosition,
    ): Boolean {
        if (actualType.parameterTypes.size != expectedType.parameterTypes.size) return false

        var hasAcceptedConstraint = false

        fun addComponentConstraint(
            actualComponent: ConeCangJieType,
            expectedComponent: ConeCangJieType,
        ): Boolean {
            val currentSubstitutor = csBuilder.buildCurrentSubstitutor().asCone()
            val actual = currentSubstitutor.substituteOrNull(actualComponent) ?: actualComponent
            val expected = currentSubstitutor.substituteOrNull(expectedComponent) ?: expectedComponent
            val actualFunction = actual.fullyExpandedType(session) as? ConeFunctionType
            val expectedFunction = expected.fullyExpandedType(session) as? ConeFunctionType
            if (actualFunction != null || expectedFunction != null) {
                if (actualFunction == null || expectedFunction == null) return false
                return addFunctionTypeArgumentConstraintsInTransaction(actualFunction, expectedFunction, position)
            }

            val actualContainsInferenceVariable = typeContainsCurrentInferenceVariable(actual)
            val expectedContainsInferenceVariable = typeContainsCurrentInferenceVariable(expected)
            if (!actualContainsInferenceVariable && !expectedContainsInferenceVariable) {
                return isInvariantArgumentCompatible(actual, expected, session)
            }

            val (currentVariableType, otherType) = when {
                isCurrentInferenceVariableType(actual) -> actual to expected
                isCurrentInferenceVariableType(expected) -> expected to actual
                else -> null to null
            }
            if (currentVariableType != null && otherType != null) {
                return csBuilder.addEqualityConstraintIfCompatible(currentVariableType, otherType, position)
            }

            val actualClassifier = actual.fullyExpandedType(session) as? ConeLookupTagBasedType ?: return false
            val expectedClassifier = expected.fullyExpandedType(session) as? ConeLookupTagBasedType ?: return false
            return addSameClassifierTypeArgumentConstraintsInTransaction(actualClassifier, expectedClassifier, position)
        }

        for ((actualParameter, expectedParameter) in actualType.parameterTypes.zip(expectedType.parameterTypes)) {
            if (!addComponentConstraint(actualParameter, expectedParameter)) return false
            if (
                typeContainsCurrentInferenceVariable(actualParameter) ||
                    typeContainsCurrentInferenceVariable(expectedParameter)
            ) {
                hasAcceptedConstraint = true
            }
        }
        if (!addComponentConstraint(actualType.returnType, expectedType.returnType)) return false
        if (
            typeContainsCurrentInferenceVariable(actualType.returnType) ||
                typeContainsCurrentInferenceVariable(expectedType.returnType)
        ) {
            hasAcceptedConstraint = true
        }
        return hasAcceptedConstraint
    }

    /**
     * 仓颉普通泛型实参是不变的；固定类型实参必须语义相等。
     */
    private fun isInvariantArgumentCompatible(
        actualType: ConeCangJieType,
        expectedType: ConeCangJieType,
        session: CfirSession,
    ): Boolean {
        if (actualType is ConeErrorType || expectedType is ConeErrorType) return true
        return AbstractTypeChecker.equalTypes(session.typeContext, actualType, expectedType)
    }

    /**
     * 当前类型树中是否含本候选约束系统尚未固定的 fresh type variable。
     *
     * 成员签名可能保留外层 lambda placeholder 或声明类型参数对应的 `ConeTypeVariableType`。
     * 这些变量不属于当前候选系统，不能作为同构类型实参下沉的目标，否则约束注入器会把
     * 外来 constructor 当作本系统变量处理。
     */
    private fun ArgumentContext.typeContainsCurrentInferenceVariable(type: ConeCangJieType): Boolean = when (type) {
        is ConeTypeVariableType -> type.typeConstructor in csBuilder.currentStorage().notFixedTypeVariables
        is ConeLookupTagBasedType -> type.typeArguments.any { typeContainsCurrentInferenceVariable(it.type) }
        is ConeFunctionType -> type.parameterTypes.any { typeContainsCurrentInferenceVariable(it) } ||
                typeContainsCurrentInferenceVariable(type.returnType)
        is ConeTupleType -> type.elementTypes.any { typeContainsCurrentInferenceVariable(it) }
        is ConeVArrayType -> typeContainsCurrentInferenceVariable(type.elementType)
        else -> false
    }

    /** 判断类型根节点是否是当前候选约束系统尚未固定的 fresh type variable。 */
    private fun ArgumentContext.isCurrentInferenceVariableType(type: ConeCangJieType): Boolean =
        type is ConeTypeVariableType && type.typeConstructor in csBuilder.currentStorage().notFixedTypeVariables

    /**
     * enum constructor payload 中的理想字面量要以 ILT equality 进入推断。
     *
     * `Some(1)` 的 owner 类型实参随后可能由外层 `Option<Int32>` 或函数值调用继续约束；
     * 因此这里保留 `IdealInt` 本身，而不是提前落成默认 `Int64`。
     */
    private fun ArgumentContext.addIdealLiteralEqualityConstraintForCurrentInferenceVariable(
        argumentType: ConeCangJieType,
        expectedType: ConeCangJieType,
        position: ConstraintPosition,
    ): Boolean {
        if (!isCurrentInferenceVariableType(expectedType)) return false
        if (!argumentType.isIdealLiteralOrPrimitive()) return false
        return csBuilder.addEqualityConstraintIfCompatible(expectedType, argumentType, position)
    }

    /**
     * 无上下文局部 lambda 的参数 placeholder 在函数值调用处需要承接实参形状。
     *
     * 对 `f(Some(1))` 这类调用，形参 `_RP0` 不能只得到一个整体 subtype 探测结果；
     * 它必须保留 `Option<T>` 这类 nominal 结构，后续 completion 才能在 `T` 固定后
     * 把最终参数类型写回 lambda header。
     */
    private fun ArgumentContext.addLocalLambdaParameterShapeConstraint(
        argumentType: ConeCangJieType,
        expectedType: ConeCangJieType,
        position: ConstraintPosition,
    ): Boolean {
        val expectedVariableType = expectedType as? ConeTypeVariableType ?: return false
        val expectedVariable = csBuilder.currentStorage()
            .allTypeVariables[expectedVariableType.typeConstructor]
        if (expectedVariable !is ConeTypeVariableForLambdaParameterType) return false
        if (!argumentType.hasCallableValueArgumentShape()) return false
        return csBuilder.addEqualityConstraintIfCompatible(expectedType, argumentType, position)
    }

    /**
     * 无参 enum constructor 在泛型函数值调用中可以先把 owner 形状写入当前类型变量。
     *
     * 例如 `fold(f)(Nil)` 的第二次调用参数期望类型是 `fold` 的 `B`；`Nil` 本身
     * 只能给出 `List<α>` 形状，真正的 `α` 需要后续 lambda 返回值或外层 expected type
     * 继续固定。这里登记 `B == List<α>`，让同一候选 completion 统一收敛；若最终仍
     * 无法固定 owner 泛型，裸 enum constructor checker 会继续报告缺少类型实参。
     */
    private fun ArgumentContext.addNoArgEnumConstructorShapeConstraintForCurrentInferenceVariable(
        expression: CfirExpression,
        argumentType: ConeCangJieType,
        expectedType: ConeCangJieType,
        position: ConstraintPosition,
    ): Boolean {
        if (!isCurrentInferenceVariableType(expectedType)) return false
        if (!expression.isNoArgEnumConstructorAccess()) return false
        if (!argumentType.hasCallableValueArgumentShape()) return false
        return csBuilder.addEqualityConstraintIfCompatible(expectedType, argumentType, position)
    }

    /** 判断表达式是否是无显式类型实参的无参 enum constructor 访问。 */
    private fun CfirExpression.isNoArgEnumConstructorAccess(): Boolean {
        val access = this as? CfirNamedAccessExpression ?: return false
        if (access.typeArguments.isNotEmpty()) return false
        val symbol = when (val reference = access.calleeReference) {
            is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol
            is CfirResolvedErrorReference -> reference.resolvedSymbol
            is CfirResolvedNamedReference -> reference.resolvedSymbol
            is CfirResolvedAppliedCallableReference -> reference.resolvedSymbol
            else -> null
        } ?: return false
        val enumConstructor = symbol.takeIf { it.isBound }?.cfir as? CfirEnumConstructor ?: return false
        return enumConstructor.valueParameters.isEmpty()
    }

    /** 是否是理想字面量或 primitive 形式的 IdealInt/IdealFloat。 */
    private fun ConeCangJieType.isIdealLiteralOrPrimitive(): Boolean =
        this is ConeIdealLiteralType || this is ConePrimitiveType && kind.isIdeal

    /** 函数值调用实参中可写回 lambda 参数 placeholder 的稳定类型形状。 */
    private fun ConeCangJieType.hasCallableValueArgumentShape(): Boolean = when (this) {
        is ConeLookupTagBasedType,
        is ConeFunctionType,
        is ConeTupleType,
        is ConeVArrayType,
        -> true

        else -> false
    }

    /** 当前候选是否是可从 payload 参与 owner 泛型推断的 enum constructor。 */
    private fun Candidate.isEnumConstructorPayloadInference(): Boolean {
        val enumConstructor = symbol.takeIf { it.isBound }?.cfir as? CfirEnumConstructor ?: return false
        return enumConstructor.valueParameters.isNotEmpty() && !callInfo.hasExplicitTypeArguments
    }

    /**
     * 函数声明名在函数类型上下文中作为引用实参时，签名不适用应归入 callable-reference 诊断。
     */
    private fun CfirExpression.isFunctionDeclarationReferenceArgument(): Boolean {
        if (this is CfirFunctionCall) return false
        val access = this as? CfirNamedAccessExpression ?: return false
        val symbol = when (val reference = access.calleeReference) {
            is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol
            is CfirResolvedNamedReference -> reference.resolvedSymbol
            else -> null
        }
        return symbol?.takeIf { it.isBound }?.cfir is CfirFunction
    }

    /**
     * 官方 LocalTypeArgumentSynthesis 会让 IdealInt/IdealFloat 先参与泛型参数推断，
     * 再由后续目标类型或其他约束把 ideal 类型收束成具体原始类型。
     * 因此实参检查不能在 `IdealInt <: T` 这类声明类型参数位置提前报参数不匹配。
     */
    private fun ConeCangJieType.isIdealTypeForTypeParameterExpectedType(
        expectedType: ConeCangJieType,
    ): Boolean {
        if (expectedType !is ConeTypeParameterType) return false
        return this.isIdealLiteralOrPrimitive()
    }

    /**
     * 局部无上下文 lambda initializer 的函数值调用中，形参 expected type 可能是当前
     * 候选系统导入的 placeholder type variable。官方 ideal literal 仍应作为推断输入，
     * 因此在底层 subtype 探测不能直接接受 `IdealInt <: α` 时，使用其默认 primitive
     * 类型继续约束该 placeholder。
     */
    private fun ArgumentContext.addIdealLiteralConstraintForCurrentInferenceVariable(
        argumentType: ConeCangJieType,
        expectedType: ConeCangJieType,
        position: ConstraintPosition,
    ): Boolean {
        if (!isCurrentInferenceVariableType(expectedType)) return false
        if (argumentType !is ConeIdealLiteralType && (argumentType !is ConePrimitiveType || !argumentType.kind.isIdeal)) {
            return false
        }
        /*
         * 泛型返回目标会在实参检查前为当前 fresh variable 注入 concrete primitive
         * upper bound。理想字面量必须以该上下文 primitive 参与下界约束，不能无条件
         * 退化为默认 Int64/Float64；否则 `id(1): Int32` 会同时得到 `Int64 <: T` 与
         * `T <: Int32`，把本应由 expected type 定型的调用错误地判为不可推断。
         */
        val approximatedArgumentType = contextualPrimitiveTargetForIdealLiteral(argumentType)
            ?: IdealTypeResolver.resolveIfIdeal(argumentType)
        return csBuilder.addSubtypeConstraintIfCompatible(approximatedArgumentType, expectedType, position)
    }

    /**
     * 从当前 fresh variable 的确定 primitive 上界中取得唯一的字面量上下文目标。
     *
     * 只接受 upper/equality 约束中的唯一 concrete primitive；如只有 Any、类型参数或多个
     * 不同 primitive 约束，保持既有默认 ideal 近似，避免把不确定上下文伪造成目标类型。
     */
    private fun ArgumentContext.contextualPrimitiveTargetForIdealLiteral(
        argumentType: ConeCangJieType,
    ): ConePrimitiveType? {
        val variableType = expectedType as? ConeTypeVariableType ?: return null
        val constraints = csBuilder.currentStorage()
            .notFixedTypeVariables[variableType.typeConstructor]
            ?.constraints
            .orEmpty()
        return constraints.asSequence()
            .filter { constraint ->
                constraint.kind == ConstraintKind.UPPER || constraint.kind == ConstraintKind.EQUALITY
            }
            .mapNotNull { constraint -> constraint.type as? ConePrimitiveType }
            .filter { primitiveType -> !primitiveType.kind.isIdeal }
            .distinct()
            .singleOrNull()
            ?.takeIf { targetType ->
                /*
                 * 目标类型只能细化同一 ideal 数值族：IdealInt 可成为具体整数，
                 * IdealFloat 可成为具体浮点。不能把 `1.1` 改写为 Int64，否则
                 * `Float64 <: T` 这个真实推断输入会丢失，进而掩盖约束矛盾。
                 */
                IdealTypeResolver.resolveIfIdeal(argumentType, targetType) == targetType
            }
    }

    /**
     * 判断当前实参检查是否应运行转换流程。
     */
    private fun  ArgumentContext.shouldRunConversion(): Boolean {
        // Currently, we only apply conversions for arguments, not lambda's return expressions
//        if (anonymousFunctionIfReturnExpression != null) {
//            // For latest LV it's equal to `return false`
//            return !LanguageFeature.DoNotRunSuspendConversionForLambdaReturnStatements.isEnabled()
//        }
        return true
    }

    /**
     * 预处理 lambda 实参，必要时创建 postponed lambda atom。
     */
    private fun ArgumentContext.preprocessLambdaArgument(atom: ConeResolutionAtomWithPostponedChild) {
        if (createLambdaWithTypeVariableAsExpectedTypeAtomIfNeeded(atom)) return
        createResolvedLambdaAtom(atom, duringCompletion = false, returnTypeVariable = null)
    }

    /**
     * 当 lambda 的期望类型本身是类型变量时创建可修订期望类型 atom。
     */
    private fun ArgumentContext.createLambdaWithTypeVariableAsExpectedTypeAtomIfNeeded(
        atom: ConeResolutionAtomWithPostponedChild,
    ): Boolean {
        val expectedType = expectedType as? ConeTypeVariableType ?: return false
        val explicitTypeArgument = csBuilder.currentStorage()
            .notFixedTypeVariables[expectedType.typeConstructor]
            ?.constraints
            ?.find { constraint ->
                constraint.kind == ConstraintKind.EQUALITY &&
                    constraint.position.from is ConeExplicitTypeParameterConstraintPosition
            }
            ?.type as? ConeCangJieType

        if (explicitTypeArgument != null && explicitTypeArgument.typeArguments.isEmpty()) {
            return false
        }

        val recoveryExpectedFunctionType = declaredFunctionUpperBoundForLambdaRecovery(expectedType)
        if (recoveryExpectedFunctionType != null) {
            /*
             * 非法 function upper bound 不能重新注册为 T 的正常 upper constraint；否则会
             * 污染 subtype/member 语义。但声明检查只否定上界 legality，不会抹掉该 bound
             * 为 lambda 提供的参数/返回形状。resolved atom 因此使用 recovery shape，
             * lambdaType <: T 仍写入当前调用约束系统，两套语义保持分离。
             */
            createResolvedLambdaAtom(
                atom = atom,
                duringCompletion = false,
                returnTypeVariable = null,
                recoveryExpectedFunctionType = recoveryExpectedFunctionType,
            )
            return true
        }

        val lambdaAtom = ConeLambdaWithTypeVariableAsExpectedTypeAtom(
            expression = atom.lambdaExpression,
            anonymousFunction = atom.lambdaExpression.anonymousFunction,
            expectedType = expectedType,
            candidateOfOuterCall = candidate,
            anonymousFunctionIfReturnExpression = anonymousFunctionIfReturnExpression,
        )
        val lambdaFunctionShape = buildLambdaFunctionShapeForTypeVariableExpectedType(atom.lambdaExpression)
        csBuilder.addSubtypeConstraint(
            lambdaFunctionShape,
            expectedType,
            ConeArgumentConstraintPosition(atom.lambdaExpression),
        )
        lambdaAtom.reviseExpectedType(lambdaFunctionShape)
        candidate.addPostponedAtom(lambdaAtom)
        atom.setPostponedSubAtom(lambdaAtom)
        return true
    }

    /**
     * 从 fresh type variable 对应的声明参数中恢复 lambda 所需的函数形状。
     *
     * 该函数只读取已经完成类型解析的声明 bound，并应用候选 substitutor；返回值只供
     * lambda atom 定型，不会进入类型参数的正常 upper-bound constraint 集合。
     */
    private fun ArgumentContext.declaredFunctionUpperBoundForLambdaRecovery(
        expectedType: ConeTypeVariableType,
    ): ConeFunctionType? {
        val originalTypeParameter = expectedType.typeConstructor.originalTypeParameter as? ConeTypeParameterLookupTag
            ?: return null
        return originalTypeParameter
            .declaredUpperBoundRefsAfterTypeResolve()
            .asSequence()
            .mapNotNull { boundRef -> boundRef.declaredUpperBoundConeTypeOrNull() }
            .map { boundType -> candidate.substitutor.substituteOrSelf(boundType).fullyExpandedType(session) }
            .filterIsInstance<ConeFunctionType>()
            .firstOrNull()
    }

    /**
     * expected type 仍是类型变量时，先把 lambda 自身的函数值形状登记进约束系统。
     *
     * 这条结构约束必须早于 completion 固定 expected 变量，否则无上下文 lambda 实参会在
     * Stage 8 被当成缺少参数类型处理，来不及通过 body 中的表达式继续推断参数类型。
     */
    private fun ArgumentContext.buildLambdaFunctionShapeForTypeVariableExpectedType(
        expression: CfirAnonymousFunctionExpression,
    ): ConeFunctionType {
        val anonymousFunction = expression.anonymousFunction
        val parameterTypes = anonymousFunction.valueParameters.mapIndexed { index, parameter ->
            parameter.returnTypeRef.coneTypeOrNull
                ?: ConeTypeVariableForLambdaParameterType(
                    TYPE_VARIABLE_NAME_PREFIX_FOR_LAMBDA_PARAMETER_TYPE + index,
                ).also(csBuilder::registerVariable).defaultType
        }
        val returnType = anonymousFunction.returnTypeRef.coneTypeOrNull
            ?: ConeTypeVariableForLambdaReturnType(
                anonymousFunction,
                TYPE_VARIABLE_NAME_FOR_LAMBDA_RETURN_TYPE,
            ).also(csBuilder::registerVariable).defaultType
        return ConeFunctionType(
            parameterTypes = parameterTypes,
            returnType = returnType,
        )
    }

    /**
     * 创建已解析 lambda atom，并把对应函数类型约束写入候选约束系统。
     */
    private fun ArgumentContext.createResolvedLambdaAtom(
        atom: ConeResolutionAtomWithPostponedChild,
        duringCompletion: Boolean,
        returnTypeVariable: ConeTypeVariableForLambdaReturnType?,
        recoveryExpectedFunctionType: ConeFunctionType? = null,
    ): ConeResolvedLambdaAtom {
        val expression = atom.lambdaExpression
        val anonymousFunction = expression.anonymousFunction
        val expectedFunctionType = recoveryExpectedFunctionType
            ?: expectedType?.fullyExpandedType(session) as? ConeFunctionType

        val declaredParameterTypes = anonymousFunction.valueParameters.mapIndexed { index, parameter ->
            parameter.returnTypeRef.coneTypeOrNull
                ?: expectedFunctionType?.parameterTypes?.getOrNull(index)
                ?: ConeTypeVariableForLambdaParameterType(
                    TYPE_VARIABLE_NAME_PREFIX_FOR_LAMBDA_PARAMETER_TYPE + index,
                ).also(csBuilder::registerVariable).defaultType
        }
        val shapeDiagnostic = expectedFunctionType
            ?.let { lambdaParameterShapeDiagnostic(anonymousFunction, it, declaredParameterTypes) }
        if (shapeDiagnostic != null) {
            anonymousFunction.lambdaParameterShapeExpectedFunctionType = expectedFunctionType
        }
        val parameterTypes = if (shapeDiagnostic is LambdaParameterTypeMismatch) {
            declaredParameterTypes.mapIndexed { index, declaredType ->
                expectedFunctionType?.parameterTypes?.getOrNull(index) ?: declaredType
            }
        } else {
            declaredParameterTypes
        }

        val createdReturnTypeVariable = if (anonymousFunction.returnTypeRef.coneTypeOrNull == null &&
            returnTypeVariable == null &&
            expectedFunctionType == null
        ) {
            ConeTypeVariableForLambdaReturnType(anonymousFunction, TYPE_VARIABLE_NAME_FOR_LAMBDA_RETURN_TYPE)
                .also(csBuilder::registerVariable)
        } else {
            null
        }

        val lambdaReturnType = expectedFunctionType?.returnType
            ?: returnTypeVariable?.defaultType
            ?: anonymousFunction.returnTypeRef.coneTypeOrNull
            ?: createdReturnTypeVariable!!.defaultType

        val resolvedAtom = ConeResolvedLambdaAtom(
            expression = expression,
            anonymousFunction = anonymousFunction,
            expectedType = expectedType,
            parameterTypes = parameterTypes,
            returnType = lambdaReturnType,
            typeVariableForLambdaReturnType = returnTypeVariable ?: createdReturnTypeVariable,
        )
        atom.setPostponedSubAtom(resolvedAtom)
        candidate.addPostponedAtom(resolvedAtom)

        val targetExpectedType = expectedType
        if (targetExpectedType != null) {
            val lambdaType = ConeFunctionType(
                parameterTypes = parameterTypes,
                returnType = lambdaReturnType,
                isCFunc = expectedFunctionType?.isCFunc ?: false,
                isClosureType = expectedFunctionType?.isClosureType ?: false,
                hasVariableLenArg = expectedFunctionType?.hasVariableLenArg ?: false,
                attributes = expectedFunctionType?.attributes ?: org.cangnova.cangjie.cfir.types.ConeAttributes.Empty,
            )
            val position = ConeArgumentConstraintPosition(expression)
            if (expectedFunctionType == null) {
                /*
                 * 非函数 expected type 下，lambda 的返回类型此时通常仍是 fresh variable。
                 * 过早写入 `(P...) -> R <: Expected` 可能先被约束系统接受，直到 body
                 * 把 R 固定后才产生无结构化诊断的迟发 contradiction。这里保留 resolved
                 * lambda atom，由 PostponedArgumentsAnalyzer 在 body 完成、实际返回类型
                 * 已知后合成最终函数值类型并执行普通实参兼容性检查。
                 */
            } else if (duringCompletion) {
                csBuilder.addSubtypeConstraint(lambdaType, targetExpectedType, position)
            } else {
                val compatible = csBuilder.isSubtypeConstraintCompatible(lambdaType, targetExpectedType)
                csBuilder.addSubtypeConstraint(lambdaType, targetExpectedType, position)
                if (!compatible) {
                    if (targetExpectedType !is ConeErrorType && shapeDiagnostic == null) {
                        reportDiagnostic(
                            ArgumentTypeMismatch(
                                expectedType = targetExpectedType,
                                actualType = lambdaType,
                                argument = expression,
                                isMismatchDueToNullability = false,
                                anonymousFunctionIfReturnExpression = anonymousFunctionIfReturnExpression,
                                systemHadContradiction = csBuilder.hasContradiction,
                            ),
                        )
                    }
                }
            }
        }

        return resolvedAtom
    }

    /**
     * 检查 lambda 头部与目标函数类型的形状规则。
     *
     * 官方 `ChkLamParamTys` 先处理参数个数，再处理显式参数类型。含省略参数时，
     * 这些错误要作为 lambda 头部错误释放，不能退化成参数类型注解缺失或 body 级联错误。
     */
    private fun ArgumentContext.lambdaParameterShapeDiagnostic(
        anonymousFunction: CfirAnonymousFunction,
        expectedFunctionType: ConeFunctionType,
        declaredParameterTypes: List<ConeCangJieType>,
    ): ResolutionDiagnostic? {
        val valueParameters = anonymousFunction.valueParameters
        val hasOmittedParameterType = valueParameters.any { it.hasOmittedLambdaParameterType() }

        if (valueParameters.size != expectedFunctionType.parameterTypes.size) {
            return if (hasOmittedParameterType) {
                LambdaParameterCountMismatch(
                    anonymousFunction = anonymousFunction,
                    expectedCount = expectedFunctionType.parameterTypes.size,
                    actualCount = valueParameters.size,
                )
            } else {
                null
            }
        }

        if (!hasOmittedParameterType) return null
        valueParameters.forEachIndexed { index, parameter ->
            if (parameter.hasOmittedLambdaParameterType()) return@forEachIndexed
            val actualType = declaredParameterTypes.getOrNull(index) ?: return@forEachIndexed
            val expectedType = expectedFunctionType.parameterTypes.getOrNull(index) ?: return@forEachIndexed
            if (actualType is ConeErrorType || expectedType is ConeErrorType) return@forEachIndexed
            if (!isLambdaTargetParameterSubtypeOfAnnotation(session, expectedType, actualType)) {
                return LambdaParameterTypeMismatch(
                    anonymousFunction = anonymousFunction,
                    parameter = parameter,
                    expectedType = expectedType,
                    actualType = actualType,
                )
            }
        }

        return null
    }

    /** 源码中是否省略了 lambda 参数类型。 */
    private fun CfirValueParameter.hasOmittedLambdaParameterType(): Boolean =
        isLambdaParameterTypeOmitted == true ||
                returnTypeRef.source?.kind == CjFakeSourceElementKind.ImplicitReturnTypeOfLambdaValueParameter

    /**
     * 将 postponed child atom 的表达式读取为匿名函数表达式。
     */
    private val ConeResolutionAtomWithPostponedChild.lambdaExpression: CfirAnonymousFunctionExpression
        get() = expression as? CfirAnonymousFunctionExpression
            ?: error("Expected anonymous function expression, but was ${expression::class}")
}
