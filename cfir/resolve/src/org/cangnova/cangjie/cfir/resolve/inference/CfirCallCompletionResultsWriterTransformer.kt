/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirFunctionTarget
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.SessionAndScopeSessionHolder
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.diagnostic.ConeCannotInferGenericFunctionTypeParameterType
import org.cangnova.cangjie.cfir.diagnostic.ConeCannotInferTypeParameterType
import org.cangnova.cangjie.cfir.diagnostic.ConeCannotInferValueParameterType
import org.cangnova.cangjie.cfir.diagnostic.ConeUnableToInferGenericFuncError
import org.cangnova.cangjie.cfir.diagnostic.ConeConstraintSystemHasContradiction
import org.cangnova.cangjie.cfir.diagnostic.ConeInapplicableCandidateError
import org.cangnova.cangjie.cfir.diagnostic.ConeTypeParameterInQualifiedAccess
import org.cangnova.cangjie.cfir.diagnostics.CfirDiagnosticHolder
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.buildResolvedArgumentList
import org.cangnova.cangjie.cfir.expressions.builder.buildArrayLiteral
import org.cangnova.cangjie.cfir.expressions.builder.buildBlockCopy
import org.cangnova.cangjie.cfir.expressions.builder.buildReturnExpression
import org.cangnova.cangjie.cfir.lookupTracker
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.render
import org.cangnova.cangjie.cfir.resolve.body.CfirDataFlowAnalyzer
import org.cangnova.cangjie.cfir.resolve.body.buildAppliedCallableReference
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtom
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtomWithPostponedChild
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolvedCallableReferenceAtom
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolvedLambdaAtom
import org.cangnova.cangjie.cfir.diagnostic.CallableReferenceFailureKind
import org.cangnova.cangjie.cfir.resolve.calls.applyNoArgEnumConstructorTargetType
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallKind
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirErrorReferenceWithCandidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirNamedReferenceWithCandidate
import org.cangnova.cangjie.cfir.resolve.calls.noArgEnumConstructorTargetType
import org.cangnova.cangjie.cfir.resolve.calls.substituteExplicitTypeArgumentConstraints
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeExpectedTypeConstraintPosition
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.resolve.body.CfirDeclarationsResolveTransformer
import org.cangnova.cangjie.cfir.resolve.toErrorReference
import org.cangnova.cangjie.cfir.resolve.transformers.CfirAbstractTreeTransformer
import org.cangnova.cangjie.cfir.resolve.transformers.IntegerLiteralAndOperatorApproximationTransformer
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculator
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.builder.buildErrorTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.cfir.visitors.transformSingle
import org.cangnova.cangjie.resolve.calls.inference.buildCurrentSubstitutor
import org.cangnova.cangjie.resolve.calls.tower.ApplicabilityDetail
import org.cangnova.cangjie.resolve.calls.tower.isSuccess
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintMismatch
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.fakeElement
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.type.model.TypeConstructorMarker
import org.cangnova.cangjie.types.TypeApproximatorConfiguration
import java.util.IdentityHashMap

/**
 * 调用完成结果写回转换器。
 *
 * 该 transformer 在约束系统完成后遍历调用树，把最终 substitutor、实参映射、lambda 返回类型、
 * PCLA 回调、整数字面量近似和错误候选诊断写回 CFIR 节点。
 */
class CfirCallCompletionResultsWriterTransformer(
    /** 当前 CFIR session。 */
    override val session: CfirSession,
    /** 当前 scope session。 */
    override val scopeSession: ScopeSession,
    /** 约束完成后的最终类型替换器。 */
    private val finalSubstitutor: ConeSubstitutor,
    /** 声明返回类型计算器。 */
    private val typeCalculator: ReturnTypeCalculator,
    /** 完成后类型近似器。 */
    private val typeApproximator: ConeTypeApproximator,
    /** 数据流分析门面，用于查询 lambda/函数返回表达式。 */
    private val dataFlowAnalyzer: CfirDataFlowAnalyzer,
    /** 整数字面量和操作符完成后的近似转换器。 */
    private val integerOperatorApproximator: IntegerLiteralAndOperatorApproximationTransformer,
    /** 当前 body resolve 上下文。 */
    private val context: BodyResolveContext,
    /** completion 阶段用于重算局部 lambda initializer body 的声明解析器。 */
    private val declarationsTransformer: CfirDeclarationsResolveTransformer,
    /** 写回模式。 */
    private val mode: Mode = Mode.Normal,
    /** 当前是否处在注解集合字面量解析上下文中。 */
    private var insideAnnotationContext: Boolean = false,
) : CfirAbstractTreeTransformer<ExpectedArgumentType?>(phase = CfirResolvePhase.BODY_RESOLVE),
    SessionAndScopeSessionHolder {


    /**
     * 为 qualified access 写回已选择候选、dispatch receiver、类型实参和结果类型。
     */
    private fun <T : CfirQualifiedAccessExpression> prepareQualifiedTransform(
        qualifiedAccessExpression: T,
        calleeReference: CfirNamedReferenceWithCandidate,
        forcedResultType: ConeCangJieType? = null,
        preserveResolvedReferenceForExpectedTypeRootMismatch: Boolean = false,
    ): T {
        val subCandidate = calleeReference.candidate

        val declaration = subCandidate.symbol.cfir

        val type = forcedResultType ?: if (declaration is CfirFunction && subCandidate.callInfo.callKind == CallKind.NamedValueAccess) {
            computeNamedValueFunctionType(declaration, subCandidate)
        } else if (declaration is CfirCallableDeclaration) {
            val calculated = typeCalculator.tryCalculateReturnType(declaration)
            // 与 body-resolve 使用同一个调用点 `This` 绑定，完成写回不能退回声明所属类的 `This`。
            subCandidate.bindThisTypeToCallSite(calculated.coneType)
        } else {
            // this branch is for cases when we have
            // some invalid qualified access expression itself.
            // e.g. `T::toString` where T is a generic type.
            // in these cases we should report an error on
            // the calleeReference.source which is not a fake source.
            ConeErrorType(
                when (declaration) {
                    is CfirTypeParameter -> ConeTypeParameterInQualifiedAccess(declaration.symbol)
                    else -> ConeSimpleDiagnostic("Callee reference to candidate without return type: ${declaration.render()}")
                }
            )
        }

        val resolvedReference = calleeReference.toResolvedReference(
            preserveResolvedReference =
                forcedResultType != null || preserveResolvedReferenceForExpectedTypeRootMismatch,
        )

        qualifiedAccessExpression.replaceCalleeReference(resolvedReference)
        qualifiedAccessExpression.replaceDispatchReceiver(
            subCandidate.dispatchReceiverExpression()
                ?.transformSingle(integerOperatorApproximator, null)
                ?.withCompletedEnumConstructorReceiverType(subCandidate)
        )
        qualifiedAccessExpression.replaceTypeArguments(computeTypeArguments(qualifiedAccessExpression, subCandidate))
        qualifiedAccessExpression.replaceConeTypeOrNull(type)

        runPCLARelatedTasksForCandidate(subCandidate)
        return qualifiedAccessExpression
    }

    /**
     * enum constructor 作为成员 receiver 时，候选检查阶段只把 receiver 临时定型为 owner 类型；
     * 完成后若 owner 实参已经固定，再写回 resolved reference 和最终类型。若仍有未固定变量，
     * 保留原引用诊断，避免把裸泛型 enum constructor 错误吞掉。
     */
    private fun CfirExpression.withCompletedEnumConstructorReceiverType(candidate: Candidate): CfirExpression {
        val expectedReceiverType = (candidate.symbol as? CfirCallableSymbol<*>)?.dispatchReceiverType ?: return this
        val completedReceiverType = expectedReceiverType.substituteType(candidate)
        if (completedReceiverType.containsUnresolvedTypeVariableOrError()) return this
        applyNoArgEnumConstructorTargetType(completedReceiverType, session)
        return this
    }

    /**
     * 判断目标 owner 类型是否仍含未完成推断结果。
     */
    private fun ConeCangJieType.containsUnresolvedTypeVariableOrError(): Boolean = when (this) {
        is ConeErrorType,
        is ConeTypeVariableType,
        -> true
        is ConeLookupTagBasedType -> typeArguments.any { it.type.containsUnresolvedTypeVariableOrError() }
        is ConeFunctionType -> parameterTypes.any { it.containsUnresolvedTypeVariableOrError() } ||
                returnType.containsUnresolvedTypeVariableOrError()
        is ConeTupleType -> elementTypes.any { it.containsUnresolvedTypeVariableOrError() }
        is ConeVArrayType -> elementType.containsUnresolvedTypeVariableOrError()
        is ConePointerType -> pointeeType.containsUnresolvedTypeVariableOrError()
        is ConeTypeAliasType -> typeArguments.any { it.type.containsUnresolvedTypeVariableOrError() } ||
                expandedType?.containsUnresolvedTypeVariableOrError() == true
        is ConeIntersectionType -> intersectedTypes.any { it.containsUnresolvedTypeVariableOrError() } ||
                upperBoundForApproximation?.containsUnresolvedTypeVariableOrError() == true
        is ConeUnionType -> unionTypes.any { it.containsUnresolvedTypeVariableOrError() }
        else -> false
    }

    /**
     * 仓颉允许把函数名作为值使用，例如 `let f = obj.foo`。
     * 这种访问完成后表达式类型应是函数类型，而不是 `foo` 的返回值类型；
     * 否则后续 `f()` 无法进入函数类型 `invoke` 的 tower level。
     */
    private fun computeNamedValueFunctionType(
        declaration: CfirFunction,
        candidate: Candidate,
    ): ConeCangJieType {
        val parameterTypes = declaration.valueParameters.map { parameter ->
            val parameterType = (parameter.returnTypeRef as? CfirResolvedTypeRef)?.coneType
                ?: return ConeErrorType(ConeSimpleDiagnostic("Unresolved function parameter type"))
            parameterType.substituteType(candidate)
        }

        val calculatedReturnType = typeCalculator.tryCalculateReturnType(declaration).coneType
        val returnType = finallySubstituteOrSelf(
            candidate.substitutedReturnType(calculatedReturnType),
        ).approximateThisTypeForDeclaration()
        return ConeFunctionType(parameterTypes, returnType)
    }

    /**
     * 将声明类型经候选 substitutor、候选当前完成结果和外层最终 substitutor 替换后近似为可写回类型。
     *
     * 嵌套泛型调用可能以 PARTIAL completion 暴露给外层候选；这时内层候选自己的
     * fixed type variables 不一定出现在外层 final substitutor 中。写回参数 expected type
     * 必须先提交该候选的 current substitutor，才能把 enum constructor owner 泛型传给子实参。
     */
    private fun ConeCangJieType.substituteType(
        candidate: Candidate,
        contextualExpectedType: ConeCangJieType? = null,
        // Substitutor from type variables (not type parameters)
        substitutor: ConeSubstitutor = finalSubstitutor,
    ): ConeCangJieType {
        // Type parameters are replaced with type variables
        val initialType = candidate.substitutor.substituteOrSelf(this)
        val contextuallyTyped = candidate.enumConstructorOwnerTargetSubstitutor(contextualExpectedType)
            ?.substituteOrNull(initialType)
            ?: initialType
        val candidateCompletedType = candidate.system.currentStorage()
            .buildCurrentSubstitutor(session.typeContext, emptyMap())
            .asCone()
            .substituteOrNull(contextuallyTyped)
            ?: contextuallyTyped
        // Type variables are replaced with final type arguments
        val substitutedType = finallySubstituteOrNull(candidateCompletedType, substitutor) ?: candidateCompletedType
        // Everything is approximated
        val finalType = typeApproximator.approximateToSuperType(
            type = substitutedType,
            TypeApproximatorConfiguration.IntermediateApproximationToSupertypeAfterCompletionInK2,
        ) ?: substitutedType

        // This is probably a temporary hack, but it seems necessary because elvis has that attribute and it may leak further like
        // fun <E> foo() = materializeNullable<E>() ?: materialize<E>() // `foo` return type unexpectedly gets inferred to @Exact E
        //
        // In FE1.0, it's not necessary since the annotation for elvis have some strange form (see org.jetbrains.kotlin.resolve.descriptorUtil.AnnotationsWithOnly)
        // that is not propagated further.
        return finalType
    }

    /**
     * 带 payload 的 enum constructor 在目标 owner 类型已知时，需要把 owner 实参投影回候选 fresh 变量。
     *
     * 这只用于完成结果写回中的 target typing：普通候选完成仍由约束系统负责；这里补齐
     * PARTIAL 嵌套调用在外层 expected type 到达后对子实参 expected type 的传递。
     */
    private fun Candidate.enumConstructorOwnerTargetSubstitutor(
        expectedType: ConeCangJieType?,
    ): ConeSubstitutor? {
        expectedType ?: return null
        val enumConstructor = symbol.takeIf { it.isBound }?.cfir as? CfirEnumConstructor ?: return null
        if (enumConstructor.valueParameters.isEmpty()) return null
        if (callInfo.hasExplicitTypeArguments) return null
        val enumConstructorSymbol = symbol as? CfirEnumConstructorSymbol ?: return null
        val ownerClassId = session.cfirProvider.getContainingClass(enumConstructorSymbol)?.classId ?: return null

        val initialOwnerArguments = substitutedReturnType()
            .fullyExpandedType(session)
            .enumTypeArgumentsForClassId(ownerClassId)
            ?: return null
        val expectedOwnerArguments = expectedType
            .fullyExpandedType(session)
            .enumTypeArgumentsForClassId(ownerClassId)
            ?: return null
        if (initialOwnerArguments.size != expectedOwnerArguments.size) return null

        val freshTypeConstructors = freshVariables.mapTo(mutableSetOf()) { it.typeConstructor }
        val substitution = linkedMapOf<TypeConstructorMarker, ConeCangJieType>()
        for ((initialArgument, expectedArgument) in initialOwnerArguments.zip(expectedOwnerArguments)) {
            val initialVariable = initialArgument as? ConeTypeVariableType
            if (initialVariable != null && initialVariable.typeConstructor in freshTypeConstructors) {
                substitution[initialVariable.typeConstructor] = expectedArgument
                continue
            }
            if (!AbstractTypeChecker.equalTypes(session.typeContext, initialArgument, expectedArgument)) return null
        }
        if (substitution.isEmpty()) return null
        return CfirTypeSubstitutorByMap(substitution)
    }

    /**
     * 在类型确实代表指定 enum owner 时抽取 owner 类型实参。
     */
    private fun ConeCangJieType.enumTypeArgumentsForClassId(classId: ClassId): List<ConeCangJieType>? = when (this) {
        is ConeEnumType -> typeArguments.map { it.type }.takeIf { this.classId == classId }
        is ConeClassLikeType -> typeArguments.map { it.type }.takeIf { this.classId == classId }
        else -> null
    }

    /**
     * 计算调用完成后应写回到实参列表中的所有实参。
     */
    private fun CfirNamedReferenceWithCandidate.computeAllArguments(
        originalArgumentList: CfirArgumentList,
        predefinedMapping: LinkedHashMap<CfirExpression, CfirValueParameter>? = null,
    ): List<CfirExpression> {
        return when {
            this.isError -> originalArgumentList.arguments
            predefinedMapping != null -> predefinedMapping.keys.toList()
            else -> candidate.argumentMapping.keys.unwrapAtoms()
        }
    }

    /**
     * 写回函数调用的候选解析结果、实参列表、结果类型和非致命诊断。
     */
    override fun transformFunctionCall(functionCall: CfirFunctionCall, data: ExpectedArgumentType?): CfirExpression {
        data?.argumentReplacements?.get(functionCall)?.let { replacement ->
            return replacement.transformSingle(this, data)
        }

        val calleeReference = functionCall.calleeReference as? CfirNamedReferenceWithCandidate ?: return functionCall
        val expectedTypeRootMismatchOnly =
            calleeReference.isExpectedTypeRootMismatchOnly(functionCall, context)
        val result = prepareQualifiedTransform(
            functionCall,
            calleeReference,
            preserveResolvedReferenceForExpectedTypeRootMismatch = expectedTypeRootMismatchOnly,
        )
        val candidate = calleeReference.candidate
        candidate.commitCallableReferenceResults()
        result.transformCompletedFunctionCallReceiver(candidate)
        val originalArgumentList = result.argumentList

        val callExpectedType = data?.getExpectedType(functionCall)
        val completedResultType = candidate.completedFunctionCallResultType(result.resolvedType, callExpectedType)
        val enumConstructorInferenceDiagnostic = candidate
            .payloadEnumConstructorInferenceDiagnostic(completedResultType)
        if (enumConstructorInferenceDiagnostic != null) {
            result.replaceCalleeReference(calleeReference.toErrorReference(enumConstructorInferenceDiagnostic))
        }
        val allArgs = calleeReference.computeAllArguments(originalArgumentList)
        val argumentMappingFailed = candidate.argumentMappingOutcome?.hasMappingFailure == true
        val (regularMapping, allArgsMapping) = if (argumentMappingFailed) {
            // 映射失败时保留原始实参结构；partial mapping 只用于解释候选失败，
            // 不能触发变参解糖或把部分实参伪装成已经完成的 resolved argument list。
            ResultingArgumentsMapping(
                regularMapping = linkedMapOf(),
                allArgsMapping = allArgs.associateWithTo(LinkedHashMap()) { null },
            )
        } else {
            candidate.handleVarargsAndReturnResultingArgumentsMapping(
                argumentList = allArgs,
                callSource = functionCall.source,
            )
        }
        val expectedArgumentsTypeMapping = candidate.createArgumentsMapping(
            forErrorReference = calleeReference.isError,
            contextualExpectedType = callExpectedType,
        )
        result.replaceArgumentList(
            rewriteArgumentList(
                candidate = candidate,
                originalArgumentList = originalArgumentList,
                expectedArgumentsTypeMapping = expectedArgumentsTypeMapping,
                regularMapping = regularMapping,
                allArgsMapping = allArgsMapping,
                forErrorReference = calleeReference.isError,
            )
        )

        val callDiagnostic = calleeReference.callDiagnosticForResultType(
            ignoreExpectedTypeRootMismatch = expectedTypeRootMismatchOnly,
        )
        val invalidChildType = result.firstInvalidReceiverOrArgumentType()
        val resultType = when {
            callDiagnostic != null -> callDiagnostic.asPropagatedCallErrorType(completedResultType)
            invalidChildType != null ->
                invalidChildType.diagnostic.asPropagatedCallErrorType(completedResultType)
            enumConstructorInferenceDiagnostic != null ->
                enumConstructorInferenceDiagnostic.asPropagatedCallErrorType(completedResultType)
            else -> completedResultType
        }
        recordExpectedTypeRootMismatch(
            expression = functionCall,
            actualType = resultType,
            expectedTypeRootMismatchOnly = expectedTypeRootMismatchOnly,
        )

        result.replaceConeTypeOrNull(resultType)
        session.lookupTracker?.recordTypeResolveAsLookup(resultType, functionCall.source, context.file.source)
        result.addNonFatalDiagnostics(candidate)
        return result
    }

    /**
     * 取得完成写回后 receiver 或实参携带的首个 InvalidTy。
     *
     * 子调用的错误必须在外层调用完成后检查：只有此时 postponed call、命名实参包装和
     * argument replacement 才已经落到最终 CFIR。delegatedType 只是恢复元数据，不参与
     * receiver scope；因此外层调用本身也必须继续保持 [ConeErrorType]。
     */
    private fun CfirFunctionCall.firstInvalidReceiverOrArgumentType(): ConeErrorType? =
        sequenceOf(explicitReceiver, dispatchReceiver)
            .plus(argumentList.arguments.asSequence())
            .mapNotNull { expression -> expression?.coneTypeOrNull as? ConeErrorType }
            .firstOrNull()

    /**
     * 把当前根诊断传播为外层调用的 InvalidTy，并仅把声明结果类型保留为恢复元数据。
     */
    private fun ConeDiagnostic.asPropagatedCallErrorType(
        completedResultType: ConeCangJieType,
    ): ConeErrorType = ConeErrorType(
        diagnostic = ConeUnreportedDuplicateDiagnostic(unwrapUnreportedDuplicateDiagnostic()),
        delegatedType = completedResultType,
    )

    /** 避免 InvalidTy 逐层传播时形成嵌套的不重复上报包装。 */
    private tailrec fun ConeDiagnostic.unwrapUnreportedDuplicateDiagnostic(): ConeDiagnostic =
        if (this is ConeUnreportedDuplicateDiagnostic) {
            original.unwrapUnreportedDuplicateDiagnostic()
        } else {
            this
        }

    /**
     * 在调用完成并传播子节点 InvalidTy 后记录赋值 RHS 根的实际类型。
     *
     * frame 以对象 identity 限定根节点，调用参数、receiver 和嵌套调用不会写入外层
     * assignment outcome；错误结果不会再派生赋值类型不匹配。
     */
    private fun recordExpectedTypeRootMismatch(
        expression: CfirExpression,
        actualType: ConeCangJieType?,
        expectedTypeRootMismatchOnly: Boolean,
    ) {
        if (!expectedTypeRootMismatchOnly) return
        val actual = actualType ?: return
        if (actual is ConeErrorType) return
        val expected = context.expectedTypeForRoot(expression) ?: return
        if (AbstractTypeChecker.isSubtypeOf(session.typeContext, actual, expected) == true) return
        context.recordExpectedTypeRootMismatch(
            expression = expression,
            actualType = actual,
            primaryDiagnostic = CfirAssignmentTypeMismatchPrimaryDiagnostic.TypeMismatch,
            rhsRootValidity = CfirAssignmentRhsRootValidity.INVALID_AFTER_MISMATCH,
        )
    }

    /**
     * 函数调用不会像普通 qualified access 一样继续 transformChildren，因此显式 receiver
     * 必须在 completion writer 中单独写回。函数类型 `invoke` 的 receiver 可能是一个
     * 仍携带 postponed lambda 的调用，如 `fold { ... }(Nil)`；外层 invoke 完成后，
     * 最终 substitutor 已经固定 receiver 的函数形状，需用同一 completion 结果写回
     * receiver 子树，避免 lambda 参数和返回类型停留在候选阶段的旧占位类型上。
     */
    private fun CfirFunctionCall.transformCompletedFunctionCallReceiver(candidate: Candidate) {
        val dispatchReceiver = candidate.dispatchReceiverExpression() ?: return
        val expectedReceiverType = dispatchReceiver.coneTypeOrNull?.substituteType(candidate)
        val receiverData = expectedReceiverType?.toExpectedType(argumentReplacements = null)

        val completedReceiver = if (explicitReceiver === dispatchReceiver) {
            transformExplicitReceiver(this@CfirCallCompletionResultsWriterTransformer, receiverData)
            transformExplicitReceiver(integerOperatorApproximator, expectedReceiverType)
            explicitReceiver
        } else {
            dispatchReceiver
                .transformSingle(this@CfirCallCompletionResultsWriterTransformer, receiverData)
                .transformSingle(integerOperatorApproximator, expectedReceiverType) as? CfirExpression
        }?.withCompletedEnumConstructorReceiverType(candidate)

        replaceDispatchReceiver(completedReceiver)
    }

    /**
     * 计算函数调用完成后表达式结果类型。
     */
    private fun Candidate.completedFunctionCallResultType(
        resolvedType: ConeCangJieType,
        contextualExpectedType: ConeCangJieType?,
    ): ConeCangJieType {
        if (callInfo.callKind == CallKind.Function && symbol.cfir is CfirVariable) {
            return finallySubstituteOrSelf(substitutedReturnType())
        }
        return resolvedType.substituteType(this, contextualExpectedType)
    }

    /**
     * payload enum constructor 的 owner 泛型属于调用推断。
     *
     * `Some(None)` 中外层 `Some` 的 owner 参数在完成后会表现为返回类型内部的无法推断错误；
     * 若仍把引用写成普通 resolved reference，后续只能把它误判为裸 generic classifier。
     * 这里把该完成结果提升为调用引用诊断，使统一诊断映射在整个调用范围报告
     * `UNABLE_TO_INFER_GENERIC_FUNC`。
     */
    private fun Candidate.payloadEnumConstructorInferenceDiagnostic(
        completedResultType: ConeCangJieType,
    ): ConeDiagnostic? {
        val enumConstructor = symbol.takeIf { it.isBound }?.cfir as? CfirEnumConstructor ?: return null
        if (enumConstructor.valueParameters.isEmpty()) return null
        if (callInfo.hasExplicitTypeArguments) return null
        if (postponedAtoms.filterIsInstance<ConeResolvedCallableReferenceAtom>().any { atom ->
                atom.failureKind == CallableReferenceFailureKind.GENERIC_TYPE_ARGUMENT_REQUIRED
            }
        ) return null
        if (completedResultType.containsAnyTypeVariable()) {
            return ConeUnableToInferGenericFuncError()
        }
        return completedResultType.findGenericInferenceDiagnostic()
    }

    /** 判断完成类型是否仍保留未完成的推断变量。 */
    private fun ConeCangJieType.containsAnyTypeVariable(): Boolean = when (this) {
        is ConeTypeVariableType -> true
        else -> typeArguments.any { it.type.containsAnyTypeVariable() }
    }

    /** 从完成类型树中查找 owner 泛型无法推断诊断。 */
    private fun ConeCangJieType.findGenericInferenceDiagnostic(): ConeDiagnostic? {
        val visited = IdentityHashMap<ConeCangJieType, Boolean>()

        fun visit(type: ConeCangJieType): ConeDiagnostic? {
            if (visited.put(type, true) != null) return null
            if (type is ConeErrorType) {
                val originalDiagnostic = when (val current = type.diagnostic) {
                    is ConeUnreportedDuplicateDiagnostic -> current.original
                    else -> current
                }
                if (originalDiagnostic is ConeCannotInferGenericFunctionTypeParameterType ||
                    originalDiagnostic is ConeCannotInferTypeParameterType
                ) {
                    return originalDiagnostic
                }
                type.delegatedType?.let(::visit)?.let { return it }
            }
            for (argument in type.typeArguments) {
                visit(argument.type)?.let { return it }
            }
            return null
        }

        return visit(this)
    }

    /**
     * 重写调用实参列表，并保持普通实参与全部实参映射同步。
     */
    private fun rewriteArgumentList(
        candidate: Candidate,
        originalArgumentList: CfirArgumentList,
        expectedArgumentsTypeMapping: ExpectedArgumentType.ArgumentsMap?,
        regularMapping: LinkedHashMap<CfirExpression, CfirValueParameter>,
        allArgsMapping: LinkedHashMap<CfirExpression, CfirValueParameter?>,
        forErrorReference: Boolean,
    ): CfirArgumentList {
        val transformedRegularMapping = LinkedHashMap<CfirExpression, CfirValueParameter>(regularMapping.size)
        val transformedAllArgsMapping = LinkedHashMap<CfirExpression, CfirValueParameter?>(allArgsMapping.size)
        val transformedArguments = IdentityHashMap<CfirExpression, CfirExpression>()

        /**
         * 转换单个实参，并缓存共享实参的转换结果。
         */
        fun transform(argument: CfirExpression, parameter: CfirValueParameter?): CfirExpression =
            transformedArguments.getOrPut(argument) {
                val transformed = transformCallArgument(argument, expectedArgumentsTypeMapping)
                    .withResolvedArrayArgumentType(candidate, parameter)
                transformed
            }

        for ((argument, parameter) in allArgsMapping) {
            transformedAllArgsMapping[transform(argument, parameter)] = parameter
        }

        for ((argument, parameter) in regularMapping) {
            transformedRegularMapping[transform(argument, parameter)] = parameter
        }

        return if (forErrorReference) {
            buildArgumentListForErrorCall(originalArgumentList, transformedAllArgsMapping)
        } else {
            buildResolvedArgumentList(originalArgumentList, transformedRegularMapping)
        }
    }

    /**
     * 对空数组字面量实参写回由形参推导出的数组类型。
     */
    private fun CfirExpression.withResolvedArrayArgumentType(
        candidate: Candidate,
        parameter: CfirValueParameter?,
    ): CfirExpression {
        if (this !is CfirArrayLiteral || elements.isNotEmpty() || parameter == null) return this
        val expectedArrayType = parameter.returnTypeRef.coneTypeOrNull
            ?.substituteType(candidate)
            ?.fullyExpandedType()
            ?.takeIf { it.arrayLiteralElementType != null }
            ?: return this
        replaceConeTypeOrNull(expectedArrayType)
        return this
    }

    /**
     * 按期望实参类型转换实参表达式，并执行完成后的整数字面量/操作符近似。
     */
    private fun transformCallArgument(
        originalArgument: CfirExpression,
        expectedArgumentsTypeMapping: ExpectedArgumentType.ArgumentsMap?,
    ): CfirExpression {
        val argumentBeforeTransform =
            (expectedArgumentsTypeMapping?.argumentReplacements?.get(originalArgument) ?: originalArgument) as CfirExpression
        val transformedArgument =
            argumentBeforeTransform.transformSingle(this, expectedArgumentsTypeMapping) as CfirExpression
        val replacedAfterTransform =
            (expectedArgumentsTypeMapping?.argumentReplacements?.get(transformedArgument) ?: transformedArgument) as CfirExpression
        val expectedType = expectedArgumentsTypeMapping?.getExpectedType(originalArgument)
            ?: expectedArgumentsTypeMapping?.getExpectedType(argumentBeforeTransform)
            ?: expectedArgumentsTypeMapping?.getExpectedType(replacedAfterTransform)
        val expectedArrayType = expectedType?.fullyExpandedType()
            ?.takeIf { it.arrayLiteralElementType != null }
        if (replacedAfterTransform is CfirArrayLiteral &&
            replacedAfterTransform.elements.isEmpty() &&
            expectedArrayType != null
        ) {
            replacedAfterTransform.replaceConeTypeOrNull(expectedArrayType)
        }
        return replacedAfterTransform.transformSingle(integerOperatorApproximator, expectedType) as CfirExpression
    }

    /**
     * 为候选实参构造 expected type 映射。
     */
    private fun Candidate.createArgumentsMapping(
        forErrorReference: Boolean,
        contextualExpectedType: ConeCangJieType?,
    ): ExpectedArgumentType.ArgumentsMap? {
        val argumentMappingFailed = argumentMappingOutcome?.hasMappingFailure == true
        val lambdasReturnType = if (argumentMappingFailed) {
            emptyMap()
        } else {
            postponedAtoms.filterIsInstance<ConeResolvedLambdaAtom>().associate { atom ->
                atom.anonymousFunction to finallySubstituteOrSelf(substitutor.substituteOrSelf(atom.returnType))
            }
        }
        val arguments = LinkedHashMap<CfirElement, ConeCangJieType>()

        /**
         * 注册表达式及其匿名函数声明对应的期望类型。
         */
        fun registerExpectedType(argument: CfirExpression, expectedType: ConeCangJieType) {
            arguments[argument] = expectedType
            if (argument is CfirAnonymousFunctionExpression) {
                arguments[argument.anonymousFunction] = expectedType
            }
        }

        if (!argumentMappingFailed) {
            val cangjieVariadicParameter = cangjieVariadicParameterForCall
            for ((atom, valueParameter) in argumentMapping) {
                val parameterType = valueParameter.returnTypeRef.coneTypeOrNull
                    ?.substituteType(this, contextualExpectedType)
                    ?.let { substituteExplicitTypeArgumentConstraints(it) }
                    ?: continue
                val expectedType = if (valueParameter == cangjieVariadicParameter) {
                    parameterType.arrayElementType ?: parameterType
                } else {
                    parameterType
                }
                registerExpectedType(atom.expression, expectedType)
                val unwrappedArgument = atom.unwrapAtom()
                if (unwrappedArgument !== atom.expression) {
                    registerExpectedType(unwrappedArgument, expectedType)
                }
            }

            val argumentReplacements = this@createArgumentsMapping.argumentReplacements
            argumentReplacements?.forEach { (original, replacement) ->
                val expectedType = arguments[original] ?: return@forEach
                if (replacement is CfirExpression) {
                    registerExpectedType(replacement, expectedType)
                }
            }

            for (lambdaAtom in postponedAtoms.filterIsInstance<ConeResolvedLambdaAtom>()) {
                val originalExpectedFunctionType = lambdaAtom.expectedType as? ConeFunctionType
                val completedFunctionType = ConeFunctionType(
                    parameterTypes = lambdaAtom.parameterTypes.map { parameterType ->
                        finallySubstituteOrSelf(substitutor.substituteOrSelf(parameterType))
                    },
                    returnType = lambdasReturnType[lambdaAtom.anonymousFunction]
                        ?: finallySubstituteOrSelf(substitutor.substituteOrSelf(lambdaAtom.returnType)),
                    isCFunc = originalExpectedFunctionType?.isCFunc ?: false,
                    isClosureType = originalExpectedFunctionType?.isClosureType ?: false,
                    hasVariableLenArg = originalExpectedFunctionType?.hasVariableLenArg ?: false,
                    attributes = originalExpectedFunctionType?.attributes ?: ConeAttributes.Empty,
                )
                val existingExpectedType = arguments[lambdaAtom.expression]
                if (existingExpectedType.functionTypeForLambdaShape() == null) {
                    registerExpectedType(lambdaAtom.expression, completedFunctionType)
                }
            }
        }

        val argumentReplacements = this@createArgumentsMapping.argumentReplacements
            ?.takeUnless { argumentMappingFailed }
        if (
            !argumentMappingFailed &&
            lambdasReturnType.isEmpty() &&
            arguments.isEmpty() &&
            argumentReplacements.isNullOrEmpty()
        ) return null
        return ExpectedArgumentType.ArgumentsMap(
            map = arguments,
            lambdasReturnTypes = lambdasReturnType,
            forErrorReference = forErrorReference,
            argumentMappingFailed = argumentMappingFailed,
            argumentReplacements,
        )
    }

    /**
     * 将最终外层候选局部保存的 callable-reference 结果提交到共享 CFIR 表达式。
     *
     * 候选比较阶段不能提前写回，否则一个 overload 候选会污染其他候选看到的引用集合。
     */
    private fun Candidate.commitCallableReferenceResults() {
        for (atom in postponedAtoms.filterIsInstance<ConeResolvedCallableReferenceAtom>()) {
            val expression = atom.expression as? CfirNamedAccessExpression ?: continue
            atom.resultingReference?.let { reference -> expression.replaceCalleeReference(reference) }
            atom.resultingTypeForCallableReference?.let { resultingType ->
                expression.replaceConeTypeOrNull(
                    finallySubstituteOrSelf(substitutor.substituteOrSelf(resultingType))
                )
            }
        }
    }

    /**
     * 变参处理后的实参映射结果。
     */
    private data class ResultingArgumentsMapping(
        /** 只包含已成功映射到形参的普通实参映射。 */
        val regularMapping: LinkedHashMap<CfirExpression, CfirValueParameter>,
        /** 包含所有原始/合成实参的映射，未映射项值为空。 */
        val allArgsMapping: LinkedHashMap<CfirExpression, CfirValueParameter?>,
    )

    /**
     * 处理仓颉变参调用，并返回写回实参列表所需的映射。
     */
    private fun Candidate.handleVarargsAndReturnResultingArgumentsMapping(
        argumentList: List<CfirExpression>,
        callSource: CjSourceElement?,
        precomputedArgumentMapping: LinkedHashMap<CfirExpression, CfirValueParameter>? = null,
    ): ResultingArgumentsMapping {
        val argumentMapping = precomputedArgumentMapping ?: this.argumentMapping.unwrapAtoms()
        val variadicParameter = cangjieVariadicParameterForCall
        val hasMarkedVariadicArgument = argumentList.any { isMarkedVariadicArgument(it) }
        val hasArgumentMappedToVariadicParameter = argumentMapping.any { (_, parameter) -> parameter == variadicParameter }
        val shouldRewriteAsVariadicCall =
            variadicParameter != null && (hasMarkedVariadicArgument || !hasArgumentMappedToVariadicParameter)
        return if (variadicParameter != null && shouldRewriteAsVariadicCall) {
            val resolvedArrayType = variadicParameter.returnTypeRef.coneTypeOrNull?.substituteType(this)
                ?: ConeErrorType(ConeSimpleDiagnostic("Unresolved variadic array parameter type"))
            val argumentMappingWithAllArgs = remapArgumentsWithCangjieVararg(
                variadicParameter = variadicParameter,
                resolvedArrayType = resolvedArrayType,
                argumentMapping = argumentMapping,
                argumentList = argumentList,
                callSource = callSource,
                parameters = declaredParametersForMapping(),
                isVariadicArgument = { argument -> isMarkedVariadicArgument(argument) },
            )
            ResultingArgumentsMapping(
                argumentMappingWithAllArgs.filterValuesNotNullToLinkedMap(),
                argumentMappingWithAllArgs,
            )
        } else {
            ResultingArgumentsMapping(
                argumentMapping,
                argumentList.associateWithTo(LinkedHashMap()) { argumentMapping[it] },
            )
        }
    }

    /**
     * 判断表达式是否在实参检查阶段被确认为普通变参元素。
     */
    private fun Candidate.isMarkedVariadicArgument(argument: CfirExpression): Boolean {
        return this.argumentMapping.keys.any { atom ->
            atom.expression === argument && variadicExpectedTypeForArgument(atom) != null
        }
    }

    /**
     * 将仓颉变参实参重映射为一个合成数组字面量实参。
     */
    private fun remapArgumentsWithCangjieVararg(
        variadicParameter: CfirValueParameter,
        resolvedArrayType: ConeCangJieType,
        argumentMapping: LinkedHashMap<CfirExpression, CfirValueParameter>,
        argumentList: List<CfirExpression>,
        callSource: CjSourceElement?,
        parameters: List<CfirValueParameter>,
        isVariadicArgument: (CfirExpression) -> Boolean,
    ): LinkedHashMap<CfirExpression, CfirValueParameter?> {
        val result = LinkedHashMap<CfirExpression, CfirValueParameter?>()
        val variadicArguments = mutableListOf<CfirExpression>()
        val variadicParameterIndex = parameters.indexOf(variadicParameter)
        var variadicArrayAdded = false

        /**
         * 将当前累计的变参实参刷入合成数组字面量。
         */
        fun flushVariadicArguments(source: CjSourceElement?) {
            if (variadicArrayAdded) return
            val variadicArray = buildArrayLiteral {
                this.source = source?.fakeElement(CjFakeSourceElementKind.VarargArgument)
                coneTypeOrNull = resolvedArrayType
                elements.addAll(variadicArguments)
            }
            result[variadicArray] = variadicParameter
            variadicArguments.clear()
            variadicArrayAdded = true
        }

        for (argument in argumentList) {
            val parameter = argumentMapping[argument]
            if (parameter == variadicParameter && isVariadicArgument(argument)) {
                variadicArguments += argument
            } else {
                val parameterIndex = parameter?.let { parameters.indexOf(it) } ?: -1
                if (
                    !variadicArrayAdded &&
                    variadicParameterIndex >= 0 &&
                    parameterIndex > variadicParameterIndex
                ) {
                    flushVariadicArguments(variadicArguments.firstOrNull()?.source ?: argument.source)
                }
                result[argument] = parameter
            }
        }
        flushVariadicArguments(variadicArguments.firstOrNull()?.source ?: callSource)
        return result
    }

    /**
     * 写回命名访问表达式。
     */
    override fun transformNamedAccessExpression(
        namedAccessExpression: CfirNamedAccessExpression,
        data: ExpectedArgumentType?
    ): CfirExpression {
        data?.argumentReplacements?.get(namedAccessExpression)?.let { replacement ->
            return replacement.transformSingle(this, data)
        }
        val replacement = replacementFor(namedAccessExpression)
        if (replacement != null) {
            return replacement.transformSingle(this, data)
        }
        return transformQualifiedAccessExpression(namedAccessExpression, data)
    }

    /**
     * 写回 qualified access 表达式。
     */
    override fun transformQualifiedAccessExpression(
        qualifiedAccessExpression: CfirQualifiedAccessExpression,
        data: ExpectedArgumentType?
    ): CfirExpression {
        data?.argumentReplacements?.get(qualifiedAccessExpression)?.let { replacement ->
            return replacement.transformSingle(this, data)
        }
        val replacement = replacementFor(qualifiedAccessExpression)
        if (replacement != null) {
            return replacement.transformSingle(this, data)
        }

        data?.getExpectedType(qualifiedAccessExpression)?.let { expectedType ->
            qualifiedAccessExpression.applyNoArgEnumConstructorTargetType(expectedType, session)?.let { targetType ->
                qualifiedAccessExpression.transformChildren(this, data)
                qualifiedAccessExpression.replaceConeTypeOrNull(targetType)
                session.lookupTracker?.recordTypeResolveAsLookup(
                    targetType,
                    qualifiedAccessExpression.source,
                    context.file.source,
                )
                return qualifiedAccessExpression
            }
        }

        val calleeReference = qualifiedAccessExpression.calleeReference as? CfirNamedReferenceWithCandidate
            ?: return qualifiedAccessExpression
        val candidate = calleeReference.candidate
        /*
         * 无参 generic enum constructor 在没有同 owner 目标类型时必须保留 fresh owner variable。
         * 它不是可默认成 `Any` 的普通泛型调用：后续 generic-bare-access checker 需要该
         * 未定型变量来报告缺失的 enum owner type argument。合法的同 owner 目标定型已在
         * CfirCallCompleter/EnumConstructorTargetTyping 中先行完成，此处只保护剩余路径。
         */
        val unresolvedNoArgEnumOwnerType = qualifiedAccessExpression.coneTypeOrNull
            ?.takeIf { type -> candidate.isUnfixedNoArgEnumConstructorOwner(type) }
        val forcedResultType = data?.getExpectedType(qualifiedAccessExpression)
            ?.let { expectedType -> candidate.noArgEnumConstructorTargetType(expectedType, session) }
        val expectedTypeRootMismatchOnly =
            calleeReference.isExpectedTypeRootMismatchOnly(qualifiedAccessExpression, context)
        val result = prepareQualifiedTransform(
            qualifiedAccessExpression,
            calleeReference,
            forcedResultType,
            preserveResolvedReferenceForExpectedTypeRootMismatch = expectedTypeRootMismatchOnly,
        )
        result.transformChildren(this, data)
        val completedActualType = forcedResultType
            ?: unresolvedNoArgEnumOwnerType
            ?: result.coneTypeOrNull?.substituteType(candidate)
        val resultType = calleeReference.callDiagnosticForResultType(
            ignoreExpectedTypeRootMismatch = expectedTypeRootMismatchOnly,
        )?.let { diagnostic ->
            ConeErrorType(ConeUnreportedDuplicateDiagnostic(diagnostic))
        } ?: completedActualType
        if (resultType != null) {
            // qualified access 完成后必须写回最终替换类型；无参 enum constructor
            // 如 `None` 的 owner 泛型依赖 expected type 约束，不能保留声明原始类型。
            val approximatedType = integerOperatorApproximator.approximateType(
                resultType,
                data?.getExpectedType(qualifiedAccessExpression),
            ) ?: resultType
            val approximatedActualType = completedActualType?.let { actualType ->
                integerOperatorApproximator.approximateType(
                    actualType,
                    context.expectedTypeForRoot(qualifiedAccessExpression),
                ) ?: actualType
            }
            recordExpectedTypeRootMismatch(
                expression = qualifiedAccessExpression,
                actualType = approximatedActualType,
                expectedTypeRootMismatchOnly = expectedTypeRootMismatchOnly,
            )
            result.replaceConeTypeOrNull(approximatedType)
            session.lookupTracker?.recordTypeResolveAsLookup(
                approximatedType,
                qualifiedAccessExpression.source,
                context.file.source,
            )
        }
        if (forcedResultType == null) {
            result.addNonFatalDiagnostics(candidate)
        }
        return result
    }

    /** 判断无参 enum constructor 的 owner 泛型是否仍由当前候选的 fresh variable 表示。 */
    private fun Candidate.isUnfixedNoArgEnumConstructorOwner(type: ConeCangJieType): Boolean {
        val enumConstructor = symbol.takeIf { it.isBound }?.cfir as? CfirEnumConstructor ?: return false
        if (enumConstructor.valueParameters.isNotEmpty() || callInfo.hasExplicitTypeArguments) return false

        val notFixedTypeConstructors = system.currentStorage().notFixedTypeVariables.keys
        return type.containsUnfixedOwnerInferenceVariable(notFixedTypeConstructors)
    }

    /** 仅识别当前候选仍拥有的 inference variable，避免把外层调用的变量误保留。 */
    private fun ConeCangJieType.containsUnfixedOwnerInferenceVariable(
        notFixedTypeConstructors: Set<TypeConstructorMarker>,
    ): Boolean = when (this) {
        is ConeTypeVariableType -> typeConstructor in notFixedTypeConstructors
        is ConeLookupTagBasedType -> typeArguments.any { it.type.containsUnfixedOwnerInferenceVariable(notFixedTypeConstructors) }
        is ConeFunctionType -> parameterTypes.any { it.containsUnfixedOwnerInferenceVariable(notFixedTypeConstructors) } ||
                returnType.containsUnfixedOwnerInferenceVariable(notFixedTypeConstructors)
        is ConeTupleType -> elementTypes.any { it.containsUnfixedOwnerInferenceVariable(notFixedTypeConstructors) }
        is ConeVArrayType -> elementType.containsUnfixedOwnerInferenceVariable(notFixedTypeConstructors)
        is ConePointerType -> pointeeType.containsUnfixedOwnerInferenceVariable(notFixedTypeConstructors)
        is ConeTypeAliasType -> typeArguments.any { it.type.containsUnfixedOwnerInferenceVariable(notFixedTypeConstructors) } ||
                expandedType?.containsUnfixedOwnerInferenceVariable(notFixedTypeConstructors) == true
        else -> false
    }

    /**
     * 写回数组字面量类型，并对元素进行完成后转换。
     */
    override fun transformArrayLiteral(arrayLiteral: CfirArrayLiteral, data: ExpectedArgumentType?): CfirExpression {
        data?.argumentReplacements?.get(arrayLiteral)?.let { replacement ->
            return replacement.transformSingle(this, data)
        }
        val expectedArrayType = data?.getExpectedType(arrayLiteral)?.fullyExpandedType()
            ?.takeIf { it.arrayLiteralElementType != null }
        if (arrayLiteral.elements.isEmpty() && expectedArrayType != null) {
            arrayLiteral.replaceConeTypeOrNull(expectedArrayType)
            return arrayLiteral
        }
        arrayLiteral.transformChildren(this, data)
        arrayLiteral.replaceConeTypeOrNull(
            integerOperatorApproximator.approximateType(
                arrayLiteral.coneTypeOrNull,
                data?.getExpectedType(arrayLiteral),
            )
        )
        return arrayLiteral
    }

    /**
     * 写回 tuple 字面量类型，并把形参/上下文提供的 tuple 元素期望类型下传到每个元素。
     */
    override fun transformTupleLiteral(tupleLiteral: CfirTupleLiteral, data: ExpectedArgumentType?): CfirExpression {
        data?.argumentReplacements?.get(tupleLiteral)?.let { replacement ->
            return replacement.transformSingle(this, data)
        }
        tupleLiteral.transformAnnotations(this, data)
        val expectedElementTypes = (data?.getExpectedType(tupleLiteral)?.fullyExpandedType() as? ConeTupleType)?.elementTypes
        val elements = tupleLiteral.elements as? MutableList<CfirExpression>
            ?: error("CfirTupleLiteral elements must be mutable during call completion")
        for (index in elements.indices) {
            val elementData = expectedElementTypes
                ?.getOrNull(index)
                ?.toExpectedType(
                    argumentReplacements = data.argumentReplacements,
                    argumentMappingFailed = data.argumentMappingFailed,
                )
            elements[index] = elements[index].transformSingle(this, elementData) as CfirExpression
        }
        tupleLiteral.replaceConeTypeOrNull(
            ConeTupleType(tupleLiteral.elements.map { it.coneTypeOrNull ?: session.builtinTypes.unitType })
        )
        return tupleLiteral
    }

    /**
     * 写回 block 结果类型。
     */
    override fun transformBlock(block: CfirBlock, data: ExpectedArgumentType?): CfirExpression {
        val expectedType = data?.getExpectedType(block)
        if (expectedType != null && block.statements.singleOrNull() is CfirExpression) {
            block.transformStatements(
                this,
                expectedType.toExpectedType(
                    argumentReplacements = data.argumentReplacements,
                    argumentMappingFailed = data.argumentMappingFailed,
                ),
            )
            block.transformOtherChildren(this, data)
            block.replaceConeTypeOrNull((block.statements.single() as CfirExpression).coneTypeOrNull)
            return block
        }

        block.transformChildren(this, data)
        block.replaceConeTypeOrNull((block.statements.lastOrNull() as? CfirExpression)?.coneTypeOrNull)
        return block
    }

    /**
     * 写回包装表达式类型。
     */
    override fun transformWrappedExpression(wrappedExpression: CfirWrappedExpression, data: ExpectedArgumentType?): CfirExpression {
        val expectedType = data?.getExpectedType(wrappedExpression)
        val expressionData = expectedType?.toExpectedType(
            argumentReplacements = data.argumentReplacements,
            argumentMappingFailed = data.argumentMappingFailed,
        ) ?: data
        wrappedExpression.transformChildren(this, expressionData)
        wrappedExpression.replaceConeTypeOrNull(wrappedExpression.expression.coneTypeOrNull)
        return wrappedExpression
    }

    /**
     * 写回 range 表达式及其端点期望类型。
     */
    override fun transformRangeExpression(rangeExpression: CfirRangeExpression, data: ExpectedArgumentType?): CfirExpression {
        data?.argumentReplacements?.get(rangeExpression)?.let { replacement ->
            return replacement.transformSingle(this, data)
        }

        val expectedRangeType = data?.getExpectedType(rangeExpression)?.rangeTypeOrNull()
        val endpointExpectedType = expectedRangeType?.typeArguments?.singleOrNull()?.type
        val endpointData = endpointExpectedType?.toExpectedType(
            argumentReplacements = data?.argumentReplacements,
            argumentMappingFailed = data?.argumentMappingFailed == true,
        )
        rangeExpression.transformAnnotations(this, data)
        rangeExpression.transformStart(this, endpointData)
        rangeExpression.transformEnd(this, endpointData)
        rangeExpression.transformStep(
            this,
            ConePrimitiveType.INT64.toExpectedType(
                argumentReplacements = data?.argumentReplacements,
                argumentMappingFailed = data?.argumentMappingFailed == true,
            ),
        )
        if (expectedRangeType != null) {
            rangeExpression.replaceConeTypeOrNull(expectedRangeType)
        }
        return rangeExpression
    }

    /**
     * 完成匿名函数表达式的返回类型、参数类型和整体函数类型写回。
     */
    override fun transformAnonymousFunctionExpression(
        anonymousFunctionExpression: CfirAnonymousFunctionExpression,
        data: ExpectedArgumentType?
    ): CfirExpression {
        if (data?.argumentMappingFailed == true) {
            anonymousFunctionExpression.anonymousFunction.isInsideFailedArgumentMapping = true
        }
        finalizeAnonymousFunction(
            function = anonymousFunctionExpression.anonymousFunction,
            data = data,
            anonymousFunctionExpression = anonymousFunctionExpression,
        )
        val expectedType = data?.getExpectedType(anonymousFunctionExpression)
            ?: data?.getExpectedType(anonymousFunctionExpression.anonymousFunction)
        val anonymousFunction = anonymousFunctionExpression.anonymousFunction
        rewriteAnonymousFunctionParameterTypes(
            anonymousFunction = anonymousFunction,
            expectedType = expectedType,
            containingCallIsError = (data as? ExpectedArgumentType.ArgumentsMap)?.forErrorReference == true,
        )
        val approximatedType = integerOperatorApproximator.approximateType(
            buildLambdaType(anonymousFunction, expectedType as? ConeFunctionType),
            expectedType,
        )
        if (approximatedType != null) {
            anonymousFunction.replaceTypeRef(
                approximatedType.toCfirResolvedTypeRef(anonymousFunction.typeRef.source, anonymousFunction.typeRef),
            )
        }
        return anonymousFunctionExpression
    }

    /**
     * 补全阶段必须把已选候选的函数形参类型写回 lambda 参数。
     *
     * Kotlin FIR 在 `FirCallCompleter.LambdaAnalyzerImpl` 中完成这一步；仓颉的
     * overload-by-lambda 会在候选回滚后再由 results writer 统一落树，因此这里
     * 对同一份 expected function type 做最终写回，而不是让 checker 兜底放行。
     */
    private fun rewriteAnonymousFunctionParameterTypes(
        anonymousFunction: CfirAnonymousFunction,
        expectedType: ConeCangJieType?,
        containingCallIsError: Boolean,
    ) {
        val expectedFunctionType = expectedType.functionTypeForLambdaShape()
            ?: return

        anonymousFunction.lambdaParameterShapeExpectedFunctionType = expectedFunctionType
        anonymousFunction.replaceMatchingParameterFunctionType(expectedFunctionType)
        if (containingCallIsError) return

        anonymousFunction.valueParameters.forEachIndexed { index, parameter ->
            val parameterType = expectedFunctionType.parameterTypes.getOrNull(index)
                ?.let(::finallySubstituteOrSelf)
                ?.let(::approximateLambdaInputType)
                ?: ConeErrorType(
                    ConeCannotInferValueParameterType(
                        parameter.symbol,
                        "Lambda or anonymous function has more parameters than expected",
                    ),
                )
            val typeRef = if (parameter.returnTypeRef is CfirImplicitTypeRef) {
                val source = parameter.source
                    ?.fakeElement(CjFakeSourceElementKind.ImplicitReturnTypeOfLambdaValueParameter)
                parameterType.toCfirResolvedTypeRef(source)
            } else {
                val source = parameter.returnTypeRef.source
                parameter.returnTypeRef.resolvedTypeFromPrototype(parameterType, source)
            }
            parameter.replaceReturnTypeRef(typeRef)
        }
    }

    /**
     * 取得 lambda 头部诊断所使用的目标函数类型。
     *
     * 错误候选不会继续把参数类型写回 lambda header，但仍要保留官方 `ChkLamParamTys`
     * 使用的目标函数形状，供最终 checker 报告参数个数、显式参数类型和返回体错误。
     */
    private fun ConeCangJieType?.functionTypeForLambdaShape(): ConeFunctionType? {
        if (this == null) return null
        return this as? ConeFunctionType
            ?: fullyExpandedType(session) as? ConeFunctionType
    }

    /**
     * 近似 lambda 输入类型，避免把内部推断类型直接写回形参。
     */
    private fun approximateLambdaInputType(type: ConeCangJieType): ConeCangJieType {
        if (type is ConeTypeVariableType && type.typeConstructor.originalTypeParameter == null) {
            return type
        }
        return typeApproximator.approximateToSuperType(
            type,
            TypeApproximatorConfiguration.IntermediateApproximationToSupertypeAfterCompletionInK2,
        ) ?: type
    }

    /**
     * 计算候选完成后应写回的类型实参类型列表。
     */
    private fun computeTypeArgumentTypes(
        candidate: Candidate,
    ): List<ConeCangJieType> {
        val declaration = candidate.symbol.cfir as? CfirCallableDeclaration ?: return emptyList()

        return declaration.typeParameters.map {
            val typeParameter = ConeTypeParameterTypeImpl(it.symbol.toLookupTag())
            val substitution = candidate.substitutor.substituteOrSelf(typeParameter)
            finallySubstituteOrSelf(substitution).let { substitutedType ->
                typeApproximator.approximateToSuperType(
                    substitutedType, TypeApproximatorConfiguration.TypeArgumentApproximationAfterCompletionInK2,
                ) ?: substitutedType
            }
        }
    }

    /**
     * 对齐 Kotlin `FirCallCompletionResultsWriterTransformer.computeTypeArguments`：
     * 完成阶段会写回推断后的类型实参，但显式错误实参本身必须保留在树上，
     * 否则错误 type-ref 的诊断会在替换过程中被剥掉。
     */
    private fun computeTypeArguments(
        access: CfirQualifiedAccessExpression,
        candidate: Candidate,
    ): List<CfirResolvedTypeRef> {
        val typeArguments = computeTypeArgumentTypes(candidate).mapIndexed { index, type ->
            val sourceTypeArgument = candidate.typeArgumentMapping.sourceTypeRef(index)
            if (sourceTypeArgument?.coneType?.fullyExpandedType(session) is ConeErrorType) {
                return@mapIndexed sourceTypeArgument
            }

            val source = sourceTypeArgument?.source
            val delegatedTypeRef = sourceTypeArgument?.delegatedTypeRef ?: sourceTypeArgument
            when (type) {
                is ConeErrorType -> buildErrorTypeRef {
                    this.source = source
                    coneType = type
                    this.delegatedTypeRef = delegatedTypeRef
                    diagnostic = type.diagnostic
                }

                else -> buildResolvedTypeRef {
                    this.source = source
                    coneType = type
                    this.delegatedTypeRef = delegatedTypeRef
                }
            }
        }

        if (typeArguments.size >= access.typeArguments.size) return typeArguments

        return typeArguments + access.typeArguments
            .subList(typeArguments.size, access.typeArguments.size)
            .filterIsInstance<CfirResolvedTypeRef>()
    }



    /**
     * 查询表达式是否有候选记录的替换表达式。
     */
    private fun replacementFor(expression: CfirExpression): CfirExpression? {
        val candidateReference = (expression as? org.cangnova.cangjie.cfir.expressions.CfirResolvable)
            ?.calleeReference as? CfirNamedReferenceWithCandidate ?: return null
        return candidateReference.candidate.argumentReplacements?.get(expression)
    }

    /**
     * 为候选引用构造 resolved reference，必要时构造成已应用 callable reference。
     */
    private fun resolvedReferenceFor(
        calleeReference: CfirNamedReferenceWithCandidate,
        resultType: ConeCangJieType,
    ): CfirNamedReference {
        val resolved = calleeReference.toResolvedReference()
        return if (resolved is CfirResolvedNamedReference && resolved !is CfirDiagnosticHolder) {
            buildAppliedCallableReference(calleeReference.name, calleeReference.candidate, resultType, finalSubstitutor)
        } else {
            resolved
        }
    }

    /**
     * 执行候选关联的 PCLA 完成后写回任务。
     */
    private fun runPCLARelatedTasksForCandidate(candidate: Candidate) {
        for (postponedCall in candidate.postponedPCLACalls) {
            postponedCall.expression.transform<CfirElement, ExpectedArgumentType?>(this, null)
        }

        for (callback in candidate.onPCLACompletionResultsWritingCallbacks) {
            callback(finalSubstitutor)
        }

        for (completion in candidate.localLambdaInitializerCompletions) {
            val lambdaExpression = completion.data.lambdaExpression
            val lambda = lambdaExpression.anonymousFunction
            val shouldRestoreAndReanalyze = !completion.data.bodyReanalyzedAfterCallableValueCompletion
            val shouldReanalyze = completion.data.applyCompletionResult(
                completion.variable,
                finalSubstitutor,
                candidate.system.currentStorage(),
                restoreBodyResolveState = shouldRestoreAndReanalyze,
            )
            if (!shouldRestoreAndReanalyze || !shouldReanalyze) continue

            context.withAnonymousFunctionTowerDataContext(lambda.symbol) {
                declarationsTransformer.doTransformAnonymousFunctionBodyFromCallCompletion(
                    lambdaExpression,
                    null,
                )
            }
            context.dropContextForAnonymousFunction(lambda)
            completion.data.bodyReanalyzedAfterCallableValueCompletion = true
        }

        val typeVariablesAfterPCLATransformer = CfirTypeVariablesAfterPCLATransformer(finalSubstitutor)
        for (lambda in candidate.lambdasAnalyzedWithPCLA) {
            lambda.transformSingle(typeVariablesAfterPCLATransformer, null)
            finalizeAnonymousFunction(lambda as? CfirFunction ?: continue, null)
        }
    }

    /**
     * 完成匿名函数的返回表达式、返回类型和隐式 return 写回。
     */
    private fun finalizeAnonymousFunction(
        function: CfirFunction,
        data: ExpectedArgumentType?,
        anonymousFunctionExpression: CfirAnonymousFunctionExpression? = null,
    ) {
        val anonymousFunction = function as? CfirAnonymousFunction ?: return
        val initialReturnType = anonymousFunction.returnTypeRef.coneTypeOrNull
        val returnExpressions = dataFlowAnalyzer
            .returnExpressionsOfAnonymousFunction(anonymousFunction)
            .replacePostponedAtomsInReturnExpressions(data)
        val containingCallIsError = (data as? ExpectedArgumentType.ArgumentsMap)?.forErrorReference == true
        val expectedFunctionType =
            (data?.let { context ->
                anonymousFunctionExpression?.let(context::getExpectedType)
            } as? ConeFunctionType)
                ?: (data?.getExpectedType(anonymousFunction) as? ConeFunctionType)
        /**
         * 对齐 Kotlin FIR 的语义意图：
         * 对“作为实参传入的 lambda”，它的返回类型首先应该受参数函数类型约束，
         * 而不是盲目继承前序阶段残留在 `returnTypeRef` 上的旧值。
         *
         * 当前仓颉在若干 trailing-lambda 场景里，早期阶段会把匿名函数的返回类型
         * 暂时写成外层 `Unit` 语境，若这里继续优先读取旧值，就会把合法的
         * `(Int64) -> Int64` lambda 错误降级成 `RETURN_TYPE_MISMATCH(expected Unit)`.
         *
         * 因此只要存在参数位 expected function type，就优先以它的 return type 作为
         * 匿名函数最终定型入口；没有参数位约束时，再回退到已有 returnTypeRef。
         */
        val expectedReturnType = expectedFunctionType?.returnType?.let(::finallySubstituteOrSelf)
            ?: initialReturnType?.let(::finallySubstituteOrSelf)
            ?: if (!containingCallIsError) {
                (data as? ExpectedArgumentType.ArgumentsMap)?.lambdasReturnTypes?.get(anonymousFunction)
            } else {
                null
            }
            ?: returnExpressions.firstNotNullOfOrNull { it.expression.coneTypeOrNull }

        val newData = expectedReturnType?.toExpectedType(
            argumentReplacements = data?.argumentReplacements,
            argumentMappingFailed = data?.argumentMappingFailed == true,
        ) ?: data?.takeIf { it.argumentMappingFailed }
        for (returnExpression in returnExpressions) {
            if (newData != null) {
                returnExpression.expression.transform<CfirElement, ExpectedArgumentType?>(this, newData)
            } else {
                returnExpression.expression.transform(this, null)
            }
            returnExpression.expression.transformSingle(integerOperatorApproximator, expectedReturnType)
        }

        anonymousFunction.body?.let { body ->
            if (newData != null) {
                body.transform<CfirElement, ExpectedArgumentType?>(this, newData)
            } else {
                body.transform(this, null)
            }
            body.transformSingle(integerOperatorApproximator, expectedReturnType)
        }

        val resultReturnType = computeAnonymousFunctionReturnType(
            anonymousFunction = anonymousFunction,
            expectedReturnType = expectedReturnType,
            returnExpressions = returnExpressions,
        )
        if (initialReturnType != resultReturnType) {
            anonymousFunction.replaceReturnTypeRef(
                anonymousFunction.returnTypeRef.resolvedTypeFromPrototype(resultReturnType, anonymousFunction.source)
            )
        }
        anonymousFunction.addReturnToLastStatementIfNeeded()
    }

    /**
     * 根据匿名函数当前参数/返回类型构造函数类型。
     */
    private fun buildLambdaType(
        function: CfirFunction,
        expectedFunctionType: ConeFunctionType? = null,
    ): ConeCangJieType? {
        val parameterTypes = function.valueParameters.mapNotNull { it.returnTypeRef.coneTypeSafe<ConeCangJieType>() }
        val returnType =
            function.returnTypeRef.coneTypeSafe<ConeCangJieType>() ?: function.body?.coneTypeOrNull ?: return null
        return ConeFunctionType(
            parameterTypes = parameterTypes,
            returnType = returnType,
            isCFunc = expectedFunctionType?.isCFunc ?: false,
            isClosureType = expectedFunctionType?.isClosureType ?: false,
            hasVariableLenArg = expectedFunctionType?.hasVariableLenArg ?: false,
            attributes = expectedFunctionType?.attributes ?: org.cangnova.cangjie.cfir.types.ConeAttributes.Empty,
        )
    }

    /**
     * 计算匿名函数最终返回类型。
     */
    private fun computeAnonymousFunctionReturnType(
        anonymousFunction: CfirAnonymousFunction,
        expectedReturnType: ConeCangJieType?,
        returnExpressions: Collection<CfirDataFlowAnalyzer.CfirAnonymousFunctionReturnExpressionInfo>,
    ): ConeCangJieType {
        if (anonymousFunction.isLambda && expectedReturnType?.isUnit == true) {
            return expectedReturnType
        }

        val inferredReturnType = session.typeContext.commonSuperTypeOrNull(
            returnExpressions.mapNotNull { it.expression.coneTypeOrNull }
        ) ?: anonymousFunction.body?.coneTypeOrNull
            ?: session.builtinTypes.unitType

        return if (anonymousFunction.isLambda && expectedReturnType != null && !inferredReturnType.isUnit) {
            expectedReturnType
        } else {
            inferredReturnType
        }
    }

    /**
     * 对非 Unit lambda 的最后一个表达式补写隐式 return。
     */
    private fun CfirAnonymousFunction.addReturnToLastStatementIfNeeded() {
        val currentBody = body ?: return
        val returnType = returnTypeRef.coneTypeOrNull ?: return
        if (returnType.isUnit) return

        val lastStatement = currentBody.statements.lastOrNull() as? CfirExpression ?: return
        if (lastStatement is CfirReturnExpression) return

        val newBody = buildBlockCopy(currentBody) {
            statements.clear()
            statements.addAll(currentBody.statements.dropLast(1))
                statements.add(
                    buildReturnExpression {
                        source = (
                            lastStatement.source
                                ?: currentBody.source
                                ?: this@addReturnToLastStatementIfNeeded.source
                            )?.fakeElement(CjFakeSourceElementKind.ImplicitReturn.FromLastStatement)
                        coneTypeOrNull = lastStatement.coneTypeOrNull
                        target = CfirFunctionTarget(labelName = null, isLambda = this@addReturnToLastStatementIfNeeded.isLambda).also {
                            it.bind(this@addReturnToLastStatementIfNeeded)
                        }
                        result = lastStatement
                    }
                )
            }

        (this as? org.cangnova.cangjie.cfir.declarations.impl.CfirAnonymousFunctionImpl)?.body = newBody
    }

    /**
     * 将 return 表达式集合中的 postponed atom 替换为完成后的表达式。
     */
    private fun Collection<CfirDataFlowAnalyzer.CfirAnonymousFunctionReturnExpressionInfo>.replacePostponedAtomsInReturnExpressions(
        data: ExpectedArgumentType?,
    ): Collection<CfirDataFlowAnalyzer.CfirAnonymousFunctionReturnExpressionInfo> {
        return map { returnInfo ->
            val replacement =
                (data?.argumentReplacements?.get(returnInfo.expression) ?: replacePostponedAtom(returnInfo.expression)) as CfirExpression
            if (replacement === returnInfo.expression) {
                returnInfo
            } else {
                returnInfo.copy(expression = replacement)
            }
        }
    }

    /**
     * 替换单个 postponed atom 表达式。
     */
    private fun replacePostponedAtom(expression: CfirExpression): CfirExpression {
        val candidate = (expression as? org.cangnova.cangjie.cfir.expressions.CfirResolvable)
            ?.calleeReference as? CfirNamedReferenceWithCandidate
            ?: return expression
        val resolvedCandidate = candidate.candidate
        return when {
            expression is CfirQualifiedAccessExpression || expression is CfirNamedAccessExpression || expression is CfirFunctionCall -> {
                expression.transform<CfirElement, ExpectedArgumentType?>(this, null) as CfirExpression
            }

            else -> {
                val argumentReplacements = resolvedCandidate.argumentReplacements
                if (argumentReplacements?.containsKey(expression) == true) {
                    argumentReplacements.getValue(expression)
                } else {
                    expression
                }
            }
        }
    }

    /**
     * 根据错误引用或候选失败状态构造用于结果类型的未报告诊断。
     */
    @OptIn(ApplicabilityDetail::class)
    private fun CfirNamedReferenceWithCandidate.callDiagnosticForResultType(
        ignoreExpectedTypeRootMismatch: Boolean,
    ): ConeDiagnostic? {
        if (ignoreExpectedTypeRootMismatch) return null
        if (this is CfirErrorReferenceWithCandidate) return diagnostic
        return candidate.callFailureDiagnosticForResultType()
    }

    @OptIn(ApplicabilityDetail::class)
    private fun Candidate.callFailureDiagnosticForResultType(): ConeDiagnostic? {
        if (!lowestApplicability.isSuccess) {
            return ConeInapplicableCandidateError(lowestApplicability, this)
        }
        if (!isSuccessful) {
            require(system.hasContradiction) {
                "Candidate is not successful, but system has no contradiction"
            }
            return ConeConstraintSystemHasContradiction(this)
        }
        return null
    }

    /**
     * 是否存在额外解析错误。
     */
    private fun CfirNamedReferenceWithCandidate.hasAdditionalResolutionErrors(): Boolean = false

    /**
     * 将候选引用转换为 resolved 或 error reference。
     */
    @OptIn(ApplicabilityDetail::class)
    private fun CfirNamedReferenceWithCandidate.toResolvedReference(
        preserveResolvedReference: Boolean = false,
    ): CfirNamedReference {
        if (preserveResolvedReference) {
            return buildResolvedNamedReference {
                source = this@toResolvedReference.source
                name = this@toResolvedReference.name
                resolvedSymbol = this@toResolvedReference.candidateSymbol
            }
        }

        val errorDiagnostic = when {
            this is CfirErrorReferenceWithCandidate -> this.diagnostic
            !candidate.lowestApplicability.isSuccess ->
                ConeInapplicableCandidateError(candidate.lowestApplicability, candidate)

            !candidate.isSuccessful -> {
                require(candidate.system.hasContradiction) {
                    "Candidate is not successful, but system has no contradiction"
                }
                ConeConstraintSystemHasContradiction(candidate)

            }

            hasAdditionalResolutionErrors() ->
                ConeConstraintSystemHasContradiction(candidate)

            else -> null
        }

        return when (errorDiagnostic) {
            null -> buildResolvedNamedReference {
                source = this@toResolvedReference.source
                name = this@toResolvedReference.name
                resolvedSymbol = this@toResolvedReference.candidateSymbol
            }

            else -> toErrorReference(errorDiagnostic)
        }
    }

    /**
     * 使用最终 substitutor 替换类型；理想字面量类型在无法替换时直接近似。
     */
    private fun finallySubstituteOrNull(
        type: ConeCangJieType,
        substitutor: ConeSubstitutor = finalSubstitutor,
    ): ConeCangJieType? {
        val result = substitutor.substituteOrNull(type)
        if (result == null && type is ConeIdealLiteralType) {
            return type.approximateIntegerLiteralType()
        }
        return result?.approximateIntegerLiteralType()
    }

    /**
     * 使用最终 substitutor 替换类型，无法替换时返回原类型。
     */
    private fun finallySubstituteOrSelf(type: ConeCangJieType): ConeCangJieType {
        return finallySubstituteOrNull(type) ?: type
    }

    /**
     * completion writer 模式。
     */
    enum class Mode {
        /** 普通调用完成写回。 */
        Normal,

        // Retained only as an upstream-aligned seam. The current local direct chain has
        // no delegated-property inference session or writer-construction call site that
        // selects this mode, so this enum value is intentionally unreachable for now.
        /** 预留的委托属性完成模式。 */
        DelegatedPropertyCompletion,
    }

    /**
     * 在注解集合字面量上下文中执行 block。
     */
    private inline fun <T> withCollectionLiteralInAnnotationResolution(block: () -> T): T {
        val savedInsideAnnotationContext = insideAnnotationContext
        insideAnnotationContext = true
        return try {
            block()
        } finally {
            insideAnnotationContext = savedInsideAnnotationContext
        }
    }

    /**
     * 默认元素转换：声明节点不参与调用完成写回。
     */
    override fun <E : CfirElement> transformElement(element: E, data: ExpectedArgumentType?): E {
        if (element is CfirDeclaration) return element
        return super.transformElement(element, data)
    }
}

/**
 * 调用完成写回时下传给实参/子表达式的期望类型信息。
 */
sealed class ExpectedArgumentType(
    /** 子表达式替换映射。 */
    val argumentReplacements: Map<CfirElement, CfirExpression>?,
    /** 外层调用的参数映射已经失败，当前实参子树不得继续产生参数类型检查诊断。 */
    val argumentMappingFailed: Boolean,
) {
    /**
     * 多实参调用的期望类型映射。
     */
    class ArgumentsMap(
        /** 表达式或匿名函数声明到期望类型的映射。 */
        val map: Map<CfirElement, ConeCangJieType>,
        /** lambda 声明到推断返回类型的映射。 */
        val lambdasReturnTypes: Map<CfirAnonymousFunction, ConeCangJieType>,
        /** 当前调用是否来自错误引用。 */
        val forErrorReference: Boolean,
        /** 当前调用是否已经在参数映射阶段终止。 */
        argumentMappingFailed: Boolean,
        argumentReplacements: Map<CfirElement, CfirExpression>?,
    ) : ExpectedArgumentType(argumentReplacements, argumentMappingFailed)

    /**
     * 单一表达式期望类型。
     */
    class ExpectedType(
        /** 下传的期望类型。 */
        val type: ConeCangJieType,
        argumentReplacements: Map<CfirElement, CfirExpression>?,
        argumentMappingFailed: Boolean = false,
    ) : ExpectedArgumentType(argumentReplacements, argumentMappingFailed)
}

/**
 * 查询指定 CFIR 元素的期望类型。
 */
private fun ExpectedArgumentType.getExpectedType(argument: CfirElement): ConeCangJieType? = when (this) {
    is ExpectedArgumentType.ArgumentsMap -> map[argument]
    is ExpectedArgumentType.ExpectedType -> type
}

/**
 * 如果类型表示标准库 Range，则返回其 classifier 类型。
 */
private fun ConeCangJieType.rangeTypeOrNull(): ConeClassifierType? = when (this) {
    is ConeClassLikeType -> takeIf { classId == StdlibClassIds.Range }
    is ConeStructType -> takeIf { classId == StdlibClassIds.Range }
    is ConeTypeAliasType -> expandedType?.rangeTypeOrNull()
    else -> null
}

/**
 * 将 Cone 类型包装为 ExpectedArgumentType。
 */
private fun ConeCangJieType.toExpectedType(
    argumentReplacements: Map<CfirElement, CfirExpression>?,
    argumentMappingFailed: Boolean = false,
): ExpectedArgumentType = ExpectedArgumentType.ExpectedType(
    type = this,
    argumentReplacements = argumentReplacements,
    argumentMappingFailed = argumentMappingFailed,
)

/**
 * 近似理想整数字面量或 primitive ideal 类型。
 */
private fun ConeCangJieType.approximateIntegerLiteralType(): ConeCangJieType =
    when (this) {
        is ConeIdealLiteralType -> getApproximatedType()
        is ConePrimitiveType -> IdealTypeResolver.resolveIfIdeal(this)
        else -> this
    }

/**
 * 基于原类型引用创建 resolved/error type ref。
 */
private fun org.cangnova.cangjie.cfir.types.CfirTypeRef.resolvedTypeFromPrototype(
    type: ConeCangJieType,
    source: org.cangnova.cangjie.source.CjSourceElement?,
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
 * 将 atom 集合展开为对应表达式列表。
 */
private fun Collection<ConeResolutionAtom>.unwrapAtoms(): List<CfirExpression> {
    return map { it.unwrapAtom() }
}

/**
 * 展开单个 atom 的实际表达式。
 */
private fun ConeResolutionAtom.unwrapAtom(): CfirExpression {
    return when (this) {
//        is ConeCollectionLiteralAtom -> subAtom?.unwrapAtom() ?: expression
        is ConeResolutionAtomWithPostponedChild -> subAtom?.unwrapAtom() ?: expression
        else -> expression
    }
}

/**
 * 将以 atom 为键的映射转换为以表达式为键的映射。
 */
fun <V> LinkedHashMap<ConeResolutionAtom, V>.unwrapAtoms(): LinkedHashMap<CfirExpression, V> {
    return mapKeysToLinkedMap { it.unwrapAtom() }
}

/**
 * 过滤掉空值并保留 LinkedHashMap 顺序。
 */
private fun <K, V : Any> LinkedHashMap<K, V?>.filterValuesNotNullToLinkedMap(): LinkedHashMap<K, V> {
    val result = LinkedHashMap<K, V>()
    for ((key, value) in this) {
        if (value != null) {
            result[key] = value
        }
    }
    return result
}

/**
 * 转换 LinkedHashMap 的键并保留插入顺序。
 */
inline fun <K1, K2, V> LinkedHashMap<K1, V>.mapKeysToLinkedMap(transform: (K1) -> K2): LinkedHashMap<K2, V> {
    return mapKeysTo(LinkedHashMap()) { transform(it.key) }
}

/**
 * 追加候选的非致命诊断。
 */
internal fun CfirQualifiedAccessExpression.addNonFatalDiagnostics(candidate: Candidate){
//    TODO 用于增加非致命性错误
}
