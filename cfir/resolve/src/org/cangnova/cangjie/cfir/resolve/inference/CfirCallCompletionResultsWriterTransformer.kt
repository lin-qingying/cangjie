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
import org.cangnova.cangjie.cfir.diagnostic.*
import org.cangnova.cangjie.cfir.diagnostics.CfirDiagnosticHolder
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.builder.buildArrayLiteral
import org.cangnova.cangjie.cfir.expressions.builder.buildReturnExpression
import org.cangnova.cangjie.cfir.lookupTracker
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.*
import org.cangnova.cangjie.cfir.resolve.body.CfirDataFlowAnalyzer
import org.cangnova.cangjie.cfir.resolve.body.CfirDeclarationsResolveTransformer
import org.cangnova.cangjie.cfir.resolve.body.CfirResourceSourceCompletion
import org.cangnova.cangjie.cfir.resolve.body.buildAppliedCallableReference
import org.cangnova.cangjie.cfir.resolve.calls.*
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallKind
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirUnscopedCompletionPass
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirUnscopedCompletionEntry
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirUnscopedCompletionReceipt
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirUnscopedFixedBinding
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirErrorReferenceWithCandidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirNamedReferenceWithCandidate
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeExpectedTypeConstraintPosition
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeFixVariableConstraintPosition
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.resolve.transformers.CfirAbstractTreeTransformer
import org.cangnova.cangjie.cfir.resolve.transformers.IntegerLiteralAndOperatorApproximationTransformer
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculator
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirMapTypeArguments
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.writeResultType
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.firstErrorTypeOrNull
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.firstSourceErrorOrPending
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.isPostponedInference
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.builder.buildErrorTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.cfir.visitors.transformSingle
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.resolve.calls.inference.buildCurrentSubstitutor
import org.cangnova.cangjie.resolve.calls.inference.model.NotEnoughInformationForTypeParameter
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintError
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintKind
import org.cangnova.cangjie.resolve.calls.tower.ApplicabilityDetail
import org.cangnova.cangjie.resolve.calls.tower.isSuccess
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.fakeElement
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.type.model.TypeConstructorMarker
import org.cangnova.cangjie.types.TypeApproximatorConfiguration
import java.util.*

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
    /** 非空表示实际 FULL/最终 PCLA writer；暂定 synthetic writer 不提供该系统。 */
    private val completionSystem: org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemImpl? = null,
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
                    // 官方口径（cjc 1.0.5 探针）：receiver 类型名未声明等根错误已另行报告时，
                    // 该成员访问报 undeclared identifier（UNRESOLVED_REFERENCE），而非
                    // "called object is not a function or constructor"——后者仅适用于
                    // 类型别名等真实不可调用对象的调用（call_err_alias 场景）。
                    else -> ConeUnresolvedNameError(calleeReference.name)
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
        /* 函数值 ABI 标签由声明 STATUS owner 发布，completion writer 只能读取该快照。 */
        declaration.symbol.lazyResolveToPhase(CfirResolvePhase.STATUS)
        val parameterTypes = declaration.valueParameters.map { parameter ->
            val parameterType = (parameter.returnTypeRef as? CfirResolvedTypeRef)?.coneType
                ?: return ConeErrorType(ConeSimpleDiagnostic("Unresolved function parameter type"))
            parameterType.substituteType(candidate)
        }

        val calculatedReturnType = typeCalculator.tryCalculateReturnType(declaration).coneType
        val returnType = finallySubstituteOrSelf(
            candidate.substitutedReturnType(calculatedReturnType),
        ).approximateThisTypeForDeclaration()
        return ConeFunctionType(
            parameterTypes = parameterTypes,
            returnType = returnType,
            isCFunc = declaration.interopInfo?.resolvedAbi?.isCFunction == true,
        )
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
        val candidateCompletedType = candidate.system.captureStorage()
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
        if (!shouldUseExpectedTypeForEnumConstructor(expectedType, session)) return null
        val enumConstructor = symbol.takeIf { it.isBound }?.cfir as? CfirEnumConstructor ?: return null
        if (enumConstructor.valueParameters.isEmpty()) return null
        if (callInfo.hasExplicitTypeArguments) return null
        val enumConstructorSymbol = symbol as? CfirEnumConstructorSymbol ?: return null
        val ownerClassId = session.cfirProvider.getContainingClass(enumConstructorSymbol)?.classId ?: return null

        val initialOwnerArguments = substitutedReturnTypeFromCalculator()
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
    /** 注解的参数列表只完成一次；解析后映射与语法实参分别保存。 */
    override fun transformAnnotationCall(annotationCall: CfirAnnotationCall, data: ExpectedArgumentType?): CfirAnnotationCall {
        val reference = annotationCall.calleeReference as? CfirNamedReferenceWithCandidate ?: return annotationCall
        val candidate = reference.candidate
        val original = annotationCall.argumentList
        val allArguments = reference.computeAllArguments(original)
        val mappingFailed = candidate.argumentMappingOutcome?.hasMappingFailure == true
        val (regularMapping, allArgsMapping) = if (mappingFailed) {
            ResultingArgumentsMapping(linkedMapOf(), allArguments.associateWithTo(LinkedHashMap()) { null })
        } else candidate.handleVarargsAndReturnResultingArgumentsMapping(
            allArguments,
            annotationCall.source,
            forErrorReference = reference.isError,
        )
        val expected = candidate.createArgumentsMapping(forErrorReference = reference.isError, contextualExpectedType = null)
        annotationCall.replaceArgumentList(rewriteArgumentList(
            candidate = candidate,
            originalArgumentList = original,
            expectedArgumentsTypeMapping = expected,
            regularMapping = regularMapping,
            allArgsMapping = allArgsMapping,
            forErrorReference = reference.isError,
        ))
        annotationCall.replaceCalleeReference(reference.toResolvedReference())
        // The completion writer only materializes the resolved argument list. The
        // declaration-owned annotation resolver publishes mapping/view/state after
        // this transform returns, so ordinary call completion remains annotation-agnostic.
        return annotationCall
    }

    override fun transformFunctionCall(functionCall: CfirFunctionCall, data: ExpectedArgumentType?): CfirExpression {
        replacementForCompletion(functionCall, data)?.let { replacement ->
            if (replacement === functionCall) return functionCall
            handleArgumentReplacementInCfg(functionCall, replacement)
            return replacement.transformSingle(this, data)
        }
        if (!prepareCandidateExpressionForCompletion(functionCall)) return functionCall
        restoreExplicitCompletionReference(functionCall)
        val calleeReference = functionCall.calleeReference as? CfirNamedReferenceWithCandidate
            ?: return functionCall.also { result -> completionCandidates[functionCall]?.let { recordFinalCompletion(it, result) } }
        if (!mayWriteCompletion(calleeReference.candidate)) return functionCall
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
        val genericInferenceDiagnostic = completedResultType
            .takeIf { candidate.isBuiltinPointerConstructorWithoutSourceTypeArguments() }
            ?.findGenericInferenceDiagnostic()
        if (genericInferenceDiagnostic != null && !calleeReference.isError) {
            result.replaceCalleeReference(calleeReference.toErrorReference(genericInferenceDiagnostic))
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
                forErrorReference = calleeReference.isError,
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
            genericInferenceDiagnostic != null ->
                genericInferenceDiagnostic.asPropagatedCallErrorType(completedResultType)
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
        recordFinalCompletion(candidate, result)
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
            .map { expression -> expression?.coneTypeOrNull as? ConeErrorType }
            .firstSourceErrorOrPending()

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
                .transformSingle(integerOperatorApproximator, expectedReceiverType)
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
            return finallySubstituteOrSelf(substitutedReturnTypeFromCalculator())
        }
        return resolvedType.substituteType(this, contextualExpectedType)
    }

    /**
     * payload enum constructor 的 owner 泛型属于调用推断。
     *
     * `Some(None)` 中外层 `Some` 的 owner 参数在完成后会表现为返回类型内部的无法推断错误；
     * 若仍把引用写成普通 resolved reference，后续只能把它误判为裸 generic classifier。
     * 这里把该完成结果提升为调用引用诊断，使统一诊断映射在 callee 范围报告
     * `UNABLE_TO_INFER_GENERIC_FUNC`。
     */
    private fun Candidate.payloadEnumConstructorInferenceDiagnostic(
        completedResultType: ConeCangJieType,
    ): ConeDiagnostic? {
        val enumConstructor = symbol.takeIf { it.isBound }?.cfir as? CfirEnumConstructor ?: return null
        if (enumConstructor.valueParameters.isEmpty()) return null
        if (callInfo.hasExplicitTypeArguments) return null
        // 裸泛型函数引用已在实参节点报告自身缺失的类型实参；不能再把该子树错误提升为外层 enum 调用。
        if (hasBareGenericFunctionReferencePayloadArgument) return null
        if (postponedAtoms.filterIsInstance<ConeResolvedCallableReferenceAtom>().any { atom ->
                atom.failureKind == CallableReferenceFailureKind.GENERIC_TYPE_ARGUMENT_REQUIRED
            }
        ) return null
        val diagnostic = if (completedResultType.containsAnyTypeVariable()) {
            ConeUnableToInferGenericFuncError()
        } else {
            completedResultType.findGenericInferenceDiagnostic()
        }
        // 官方 `GenerateTypeMappingByInference`（TypeArgumentInference.cpp:578）只在
        // `!Utils::In(ce.args, [](const auto& arg) { return !Ty::IsTyCorrect(arg->ty); })`
        // ——即没有任何实参的类型是错误类型——时才报告 `sema_unable_to_infer_generic_func`。
        // 实参自身的根诊断已经单独报告时，owner 的推断失败只是错误恢复级联（cjc 1.0.5 实测
        // `(Some(1 + true) ?? 2(false)) + true` 只报内层 invalid_binary_expr 与
        // no_match_operator_function_call，不报 Some 的 unable_to_infer）。
        if (diagnostic is ConeUnableToInferGenericFuncError && hasErrorTypedArgument()) return null
        return diagnostic
    }

    /**
     * 判断调用实参中是否已经有类型为错误类型的实参。
     *
     * `isUninferredParameter` 的 error type 是**本次推断自身**尚未求解的结果
     * （`Some(None)` 里 `None` 的类型参数），在官方对应「类型正确、但含未定类型变量」，
     * 不属于实参上已有的根诊断，因此不参与级联抑制判定。
     */
    private fun Candidate.hasErrorTypedArgument(): Boolean =
        callInfo.arguments.any { argument ->
            val errorType = argument.coneTypeOrNull as? ConeErrorType ?: return@any false
            !errorType.isUninferredParameter
        }

    /** 判断完成类型是否仍保留未完成的推断变量。 */
    private fun ConeCangJieType.containsAnyTypeVariable(): Boolean = when (this) {
        is ConeTypeVariableType -> true
        is ConeLookupTagBasedType -> typeArguments.any { it.type.containsAnyTypeVariable() }
        is ConeFunctionType -> parameterTypes.any { it.containsAnyTypeVariable() } ||
                returnType.containsAnyTypeVariable()
        is ConeTupleType -> elementTypes.any { it.containsAnyTypeVariable() }
        is ConeVArrayType -> elementType.containsAnyTypeVariable()
        is ConePointerType -> pointeeType.containsAnyTypeVariable()
        is ConeTypeAliasType -> typeArguments.any { it.type.containsAnyTypeVariable() } ||
                expandedType?.containsAnyTypeVariable() == true
        is ConeIntersectionType -> intersectedTypes.any { it.containsAnyTypeVariable() } ||
                upperBoundForApproximation?.containsAnyTypeVariable() == true
        is ConeUnionType -> unionTypes.any { it.containsAnyTypeVariable() }
        else -> false
    }

    /** 只为隐式 CPointer 构造传播 pointee 未推断诊断，显式类型实参不能走该路径。 */
    private fun Candidate.isBuiltinPointerConstructorWithoutSourceTypeArguments(): Boolean {
        val declaration = symbol.takeIf { it.isBound }?.cfir as? CfirFunction ?: return false
        if (declaration.origin != CfirDeclarationOrigin.Synthetic.BuiltinPointerConstructor) return false
        if (callInfo.hasExplicitTypeArguments) return false
        return (callInfo.callSite as? CfirQualifiedAccessExpression)
            ?.typeArguments
            ?.none { typeArgument -> typeArgument.source != null }
            ?: true
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
                if (type.isUninferredParameter) {
                    return ConeUnableToInferGenericFuncError()
                }
                if (originalDiagnostic is ConeCannotInferGenericFunctionTypeParameterType ||
                    originalDiagnostic is ConeCannotInferTypeParameterType
                ) {
                    return originalDiagnostic
                }
                type.delegatedType?.let(::visit)?.let { return it }
            }
            when (type) {
                is ConeLookupTagBasedType -> type.typeArguments.forEach { argument ->
                    visit(argument.type)?.let { return it }
                }

                is ConeFunctionType -> {
                    type.parameterTypes.forEach { parameterType -> visit(parameterType)?.let { return it } }
                    visit(type.returnType)?.let { return it }
                }

                is ConeTupleType -> type.elementTypes.forEach { elementType -> visit(elementType)?.let { return it } }
                is ConeVArrayType -> visit(type.elementType)?.let { return it }
                is ConePointerType -> visit(type.pointeeType)?.let { return it }
                is ConeTypeAliasType -> {
                    type.typeArguments.forEach { argument -> visit(argument.type)?.let { return it } }
                    type.expandedType?.let { visit(it)?.let { return it } }
                }

                is ConeIntersectionType -> {
                    type.intersectedTypes.forEach { intersectedType -> visit(intersectedType)?.let { return it } }
                    type.upperBoundForApproximation?.let { visit(it)?.let { return it } }
                }

                is ConeUnionType -> type.unionTypes.forEach { unionType -> visit(unionType)?.let { return it } }
                else -> Unit
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
        return withCallCfgOwner(candidate.callCfgOwner()) {
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

            if (forErrorReference) {
                buildArgumentListForErrorCall(originalArgumentList, transformedAllArgsMapping)
            } else {
                buildResolvedArgumentList(originalArgumentList, transformedRegularMapping)
            }
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

    /** 由实际 CallInfo 的 lexical 声明链及实际 lambda body 下传，不从 callee/source 位置推测。 */
    private var callCfgOwner: CfirControlFlowGraphOwner? = null

    private inline fun <T> withCallCfgOwner(owner: CfirControlFlowGraphOwner, action: () -> T): T {
        val previous = callCfgOwner
        callCfgOwner = owner
        return try {
            action()
        } finally {
            callCfgOwner = previous
        }
    }

    private fun Candidate.callCfgOwner(): CfirControlFlowGraphOwner =
        (sequenceOf(callInfo.containingFile) + callInfo.containingDeclarations.asSequence())
            .filterIsInstance<CfirControlFlowGraphOwner>().last()

    /** 对位 Kotlin handleArgumentReplacementInCfg：仅在实际 AST 提交点接管同次求值的 fir。 */
    @OptIn(org.cangnova.cangjie.cfir.resolve.dfa.cfg.CfgInternals::class)
    private fun handleArgumentReplacementInCfg(original: CfirExpression, replacement: CfirExpression) {
        if (original is CfirFunctionCall && replacement is CfirFunctionCall && original !== replacement) {
            dataFlowAnalyzer.updateFunctionCallNodes(original, replacement, checkNotNull(callCfgOwner) { "Call CFG replacement has no actual lexical owner" })
        }
    }

    /**
     * 按实际参数映射完成实参；字面量仅通过当前 writer 的受控入口收敛。
     */
    private fun transformCallArgument(
        originalArgument: CfirExpression,
        expectedArgumentsTypeMapping: ExpectedArgumentType.ArgumentsMap?,
    ): CfirExpression {
        val argumentBeforeTransform =
            (expectedArgumentsTypeMapping?.argumentReplacements?.get(originalArgument) ?: originalArgument)
        handleArgumentReplacementInCfg(originalArgument, argumentBeforeTransform)
        val expectedTypeBeforeTransform = expectedArgumentsTypeMapping?.getExpectedType(originalArgument)
            ?: expectedArgumentsTypeMapping?.getExpectedType(argumentBeforeTransform)
        val argumentData = if (expectedTypeBeforeTransform != null && argumentBeforeTransform !== originalArgument) {
            checkNotNull(expectedArgumentsTypeMapping).let { mapping ->
                ExpectedArgumentType.ArgumentsMap(
                    map = mapping.map + (argumentBeforeTransform to expectedTypeBeforeTransform),
                    lambdasReturnTypes = mapping.lambdasReturnTypes,
                    forErrorReference = mapping.forErrorReference,
                    argumentMappingFailed = mapping.argumentMappingFailed,
                    argumentReplacements = mapping.argumentReplacements,
                )
            }
        } else expectedArgumentsTypeMapping
        val transformedArgument =
            argumentBeforeTransform.transformSingle(this, argumentData)
        val replacedAfterTransform =
            (expectedArgumentsTypeMapping?.argumentReplacements?.get(transformedArgument) ?: transformedArgument)
        handleArgumentReplacementInCfg(argumentBeforeTransform, replacedAfterTransform)
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
        // 后置替换产生的新 literal 尚未经过 writer；其它实参不能再绕过暂定/FULL 守卫近似。
        if (replacedAfterTransform is CfirLiteralExpression && replacedAfterTransform !== transformedArgument) {
            val replacementData = expectedType?.toExpectedType(
                argumentReplacements = expectedArgumentsTypeMapping?.argumentReplacements,
                argumentMappingFailed = expectedArgumentsTypeMapping?.argumentMappingFailed == true,
            ) ?: argumentData
            return replacedAfterTransform.transformSingle(this, replacementData)
        }
        return replacedAfterTransform
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
            val restoreRegularCall = forErrorReference && cangjieVariadicRegularCallDiagnostics.isNotEmpty()
            for ((atom, valueParameter) in argumentMapping) {
                val parameterType = valueParameter.returnTypeRef.coneTypeOrNull
                    ?.substituteType(this, contextualExpectedType)
                    ?.let { substituteExplicitTypeArgumentConstraints(it) }
                    ?: continue
                // 完成阶段消费实参检查已经发布的变参状态；完整 Array 实参仍按整个形参类型完成。
                val expectedType = if (
                    !restoreRegularCall && valueParameter == cangjieVariadicParameter &&
                        variadicExpectedTypeForArgument(atom) != null
                ) {
                    requireNotNull(parameterType.fullyExpandedType(session).arrayElementType) {
                        "A confirmed Cangjie variadic parameter must have an Array element type"
                    }
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
        forErrorReference: Boolean,
        precomputedArgumentMapping: LinkedHashMap<CfirExpression, CfirValueParameter>? = null,
    ): ResultingArgumentsMapping {
        val argumentMapping = precomputedArgumentMapping ?: this.argumentMapping.unwrapAtoms()
        val variadicParameter = cangjieVariadicParameterForCall
        val hasMarkedVariadicArgument = argumentList.any { isMarkedVariadicArgument(it) }
        val hasArgumentMappedToVariadicParameter = argumentMapping.any { (_, parameter) -> parameter == variadicParameter }
        val shouldRewriteAsVariadicCall =
            !(forErrorReference && cangjieVariadicRegularCallDiagnostics.isNotEmpty()) &&
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
        replacementForCompletion(namedAccessExpression, data)?.let { replacement ->
            if (replacement === namedAccessExpression) return namedAccessExpression
            return replacement.transformSingle(this, data)
        }
        if (!prepareCandidateExpressionForCompletion(namedAccessExpression)) return namedAccessExpression
        return transformQualifiedAccessExpression(namedAccessExpression, data)
    }

    /**
     * 写回 qualified access 表达式。
     */
    override fun transformQualifiedAccessExpression(
        qualifiedAccessExpression: CfirQualifiedAccessExpression,
        data: ExpectedArgumentType?
    ): CfirExpression {
        replacementForCompletion(qualifiedAccessExpression, data)?.let { replacement ->
            if (replacement === qualifiedAccessExpression) return qualifiedAccessExpression
            return replacement.transformSingle(this, data)
        }
        if (!prepareCandidateExpressionForCompletion(qualifiedAccessExpression)) return qualifiedAccessExpression

        restoreExplicitCompletionReference(qualifiedAccessExpression)
        completionCandidates[qualifiedAccessExpression]?.let { if (!mayWriteCompletion(it)) return qualifiedAccessExpression }
        data?.getExpectedType(qualifiedAccessExpression)?.let { expectedType ->
            qualifiedAccessExpression.applyNoArgEnumConstructorTargetType(expectedType, session)?.let { targetType ->
                qualifiedAccessExpression.transformChildren(this, data)
                qualifiedAccessExpression.replaceConeTypeOrNull(targetType)
                session.lookupTracker?.recordTypeResolveAsLookup(
                    targetType,
                    qualifiedAccessExpression.source,
                    context.file.source,
                )
                completionCandidates[qualifiedAccessExpression]?.let { recordFinalCompletion(it, qualifiedAccessExpression) }
                return qualifiedAccessExpression
            }
        }

        val calleeReference = qualifiedAccessExpression.calleeReference as? CfirNamedReferenceWithCandidate
            ?: return qualifiedAccessExpression.also { result -> completionCandidates[result]?.let { recordFinalCompletion(it, result) } }
        val candidate = calleeReference.candidate
        if (!mayWriteCompletion(candidate)) return qualifiedAccessExpression
        /*
         * 无参 generic enum constructor 在没有同 owner 目标类型时必须保留 fresh owner variable。
         * 它不是可默认成 `Any` 的普通泛型调用：后续 generic-bare-access checker 需要该
         * 未定型变量来报告缺失的 enum owner type argument。合法的同 owner 目标定型已在
         * CfirCallCompleter/EnumConstructorTargetTyping 中先行完成，此处只保护剩余路径。
         */
        val unresolvedNoArgEnumOwnerType = qualifiedAccessExpression.coneTypeOrNull
            ?.takeIf { type -> candidate.isUnfixedNoArgEnumConstructorOwner(type) }
        // 只发布成功候选的最终 owner：原错误引用和未固定的目标不提供成功凭据。
        val forcedResultType = if (calleeReference.isError) null else
            candidate.withConstraintSystemForCompletionResults {
                data?.getExpectedType(qualifiedAccessExpression)
                    ?.let { expectedType -> candidate.noArgEnumConstructorTargetType(expectedType.substituteType(candidate), session) }
                    ?.takeUnless { it.containsUnresolvedTypeVariableOrError() }
            }
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
        recordFinalCompletion(candidate, result)
        return result
    }

    /** 判断无参 enum constructor 的 owner 泛型是否仍由当前候选的 fresh variable 表示。 */
    private fun Candidate.isUnfixedNoArgEnumConstructorOwner(type: ConeCangJieType): Boolean {
        val enumConstructor = symbol.takeIf { it.isBound }?.cfir as? CfirEnumConstructor ?: return false
        if (enumConstructor.valueParameters.isNotEmpty() || callInfo.hasExplicitTypeArguments) return false

        val notFixedTypeConstructors = system.captureStorage().notFixedTypeVariables.keys
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
        // 空数组在无上下文时可能携带“无法推断”占位类型，已有目标后仍须完成定型。
        val expectedArrayType = if (arrayLiteral.elements.isNotEmpty() && arrayLiteral.coneTypeOrNull is ConeErrorType) {
            null
        } else {
            data?.getExpectedType(arrayLiteral)?.let { expectedType ->
                arrayLiteral.contextualArrayLiteralTypeOrNull(expectedType, session)
            }
        }
        if (expectedArrayType != null && data != null) {
            val elementType = requireNotNull(expectedArrayType.arrayLiteralElementType)
            val elementData = elementType.toExpectedType(
                argumentReplacements = data.argumentReplacements,
                argumentMappingFailed = data.argumentMappingFailed,
            )
            arrayLiteral.transformAnnotations(this, data)
            arrayLiteral.transformElements(this, elementData)
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
     * 对位 Kotlin transformSyntheticCall/when result writer：if 自身就是 completion carrier。
     * 先写真实分支实参与 PCLA 子调用，再发布结果；条件不接受值分支的 expected function type。
     */
    override fun transformIfExpression(ifExpression: CfirIfExpression, data: ExpectedArgumentType?): CfirExpression =
        transformSyntheticCall(ifExpression, data) { checkedTarget ->
            completedIfResultType(this, checkedTarget)
        }

    override fun transformMatchExpression(matchExpression: CfirMatchExpression, data: ExpectedArgumentType?): CfirExpression =
        transformSyntheticCall(matchExpression, data) { checkedTarget ->
            completedMatchResultType(this, checkedTarget)
        }

    override fun transformTryExpression(tryExpression: CfirTryExpression, data: ExpectedArgumentType?): CfirExpression =
        transformSyntheticCall(tryExpression, data) { checkedTarget ->
            completedTryResultType(this, checkedTarget)
        }

    /**
     * 对位 Kotlin transformSyntheticCall：实际 carrier、子实参和最终引用在同一完成事务中发布。
     * 仓颉的 source-visible Join 只消费真正完成的分支；不通过改写通用错误名字代替语义检查。
     */
    private inline fun <reified D> transformSyntheticCall(
        syntheticCall: D,
        data: ExpectedArgumentType?,
        computeSourceResult: D.(ConeCangJieType?) -> ConeCangJieType,
    ): D where D : CfirResolvable, D : CfirExpression {
        if (!prepareCandidateExpressionForCompletion(syntheticCall)) return syntheticCall
        restoreExplicitCompletionReference(syntheticCall)
        val reference = syntheticCall.calleeReference as? CfirNamedReferenceWithCandidate
        if (reference == null) {
            transformSyntheticCallChildren(syntheticCall, data)
            completionCandidates[syntheticCall]?.let { recordFinalCompletion(it, syntheticCall) }
            return syntheticCall
        }
        val candidate = reference.candidate
        if (!mayWriteCompletion(candidate)) return syntheticCall
        candidate.commitCallableReferenceResults()
        val declaration = candidate.symbol.cfir as CfirNamedFunction
        val declaredType = typeCalculator.tryCalculateReturnType(declaration).coneType
        val completedCandidateType = finallySubstituteOrSelf(candidate.substitutor.substituteOrSelf(declaredType))
        syntheticCall.replaceConeTypeOrNull(completedCandidateType)

        // 先保留 source context 的原始目标：未固定 lambda R 的最终 Any 不是已检查的显式 Any 目标。
        val contextualType = (candidate.callInfo.resolutionMode as? ResolutionMode.WithExpectedType)?.expectedType
            ?: data?.getExpectedType(syntheticCall)
        val expectedType = contextualType?.let(::finallySubstituteOrSelf)
        val checkedTarget = contextualType?.takeUnless {
            it.contains { type -> type is ConeTypeVariableType || type is ConeErrorType }
        }?.let(::finallySubstituteOrSelf)
        val expectedArguments = candidate.createArgumentsMapping(reference.isError, expectedType)
        withCallCfgOwner(candidate.callCfgOwner()) {
            transformSyntheticCallChildren(syntheticCall, expectedArguments)
        }
        runPCLARelatedTasksForCandidate(candidate)

        val expectedTypeRootMismatchOnly = reference.isExpectedTypeRootMismatchOnly(syntheticCall, context)
        val callDiagnostic = reference.callDiagnosticForResultType(
            ignoreExpectedTypeRootMismatch = expectedTypeRootMismatchOnly,
        )
        // 暂定 writer 没有 FULL authority，仍保留 actual candidate；source Join 只能在正式 writer 中执行。
        val sourceResult = if (completionSystem != null) {
            val computed = syntheticCall.computeSourceResult(checkedTarget)
            if (computed !is ConeErrorType && checkedTarget != null &&
                candidate.callInfo.resolutionMode.isOperatorOperandInference &&
                preservesActualOperatorSourceType(candidate, completedCandidateType, checkedTarget)
            ) completedCandidateType else computed
        } else null
        val sourceError = sourceResult as? ConeErrorType
        val ownedTypedSourceFailure = completionSystem != null && sourceResult != null && sourceError == null &&
            callDiagnostic is ConeConstraintSystemHasContradiction && callDiagnostic.candidate === candidate &&
            isOwnedTypedSelectSourceFailure(syntheticCall, candidate, checkedTarget)
        val ownedTypedTargetConstraintFailure = completionSystem != null && sourceResult != null && sourceError == null &&
            callDiagnostic is ConeConstraintSystemHasContradiction && callDiagnostic.candidate === candidate &&
            isOwnedTypedSelectTargetConstraintFailure(syntheticCall, candidate, checkedTarget)
        if (sourceError != null) {
            check(sourceError.diagnostic.unwrapUnreportedDuplicateDiagnostic() !== ConePostponedInferenceDiagnostic) {
                "FULL select contains an unfinished source result"
            }
        }
        val resultType = when {
            sourceError != null -> ConeErrorType(
                ConeUnreportedDuplicateDiagnostic(sourceError.diagnostic.unwrapUnreportedDuplicateDiagnostic()),
                isUninferredParameter = sourceError.isUninferredParameter,
                delegatedType = sourceError.delegatedType,
                typeArguments = sourceError.typeArguments,
                attributes = sourceError.attributes,
            )
            callDiagnostic != null -> callDiagnostic.asPropagatedCallErrorType(completedCandidateType)
            sourceResult != null -> sourceResult
            else -> completedCandidateType
        }
        if (completionSystem != null || callDiagnostic != null) {
            val completedReference = if (sourceError != null) {
                // 这是 actual branch 的原诊断，普通 source generic call 的诊断不经过本通道。
                reference.toErrorReference(sourceError.diagnostic)
            } else if (ownedTypedSourceFailure || ownedTypedTargetConstraintFailure) {
                // 保留失败引用和实际 CS 错误；内部 select K 的推断失败由真实 tail 消费点报告。
                reference.toErrorReference(ConeUnreportedDuplicateDiagnostic(checkNotNull(callDiagnostic)))
            } else reference.toResolvedReference(preserveResolvedReference = expectedTypeRootMismatchOnly)
            syntheticCall.replaceCalleeReference(completedReference)
        }
        syntheticCall.replaceConeTypeOrNull(resultType)
        recordExpectedTypeRootMismatch(syntheticCall, resultType, expectedTypeRootMismatchOnly)
        session.lookupTracker?.recordTypeResolveAsLookup(resultType, syntheticCall.source, context.file.source)
        recordFinalCompletion(candidate, syntheticCall)
        return syntheticCall
    }

    /**
     * 仅识别闭合目标与实际分支值不兼容、且错误只属于本 select 返回 VM 的 NotEnough。
     * 原 completed source 消费点已先检查分支错误；这里不重复遍历或展开 source 诊断。
     * 不修改 VM、固定解或 generic 诊断；真实 source generic/lambda 错误与未决 fork 不走此通道。
     */
    private fun <D> isOwnedTypedSelectSourceFailure(
        carrier: D,
        candidate: Candidate,
        target: ConeCangJieType?,
    ): Boolean where D : CfirExpression, D : CfirResolvable {
        fun isWithdrawn(): Boolean = candidate.isSourceCompletionWithdrawn ||
            candidate.implicitTypeComputationScope?.graph?.isCandidateWithdrawn(candidate) == true
        if (completionSystem == null || target == null || candidate.callInfo.callKind != CallKind.SyntheticSelect ||
            candidate.callInfo.callSite !== carrier ||
            (carrier.calleeReference as? CfirNamedReferenceWithCandidate)?.candidate !== candidate || isWithdrawn()
        ) return false
        fun isClosedSourceType(type: ConeCangJieType): Boolean {
            var closed = true
            type.forEachTypeIncludingMetadata {
                if (it is ConeTypeVariableType || it is ConeStubType || it is ConeErrorType ||
                    it is ConeIdealLiteralType || it is ConePrimitiveType && it.kind.isIdeal
                ) closed = false
            }
            return closed
        }
        if (!isClosedSourceType(target)) return false
        val branches = when (carrier) {
            is CfirIfExpression -> listOf(carrier.thenBranch, carrier.elseBranch ?: return false)
            is CfirMatchExpression -> carrier.branches.map { it.body }
            is CfirTryExpression -> {
                if (carrier.resources.isNotEmpty() || carrier.handlers.isNotEmpty()) return false
                listOf(carrier.tryBlock) + carrier.catches.map { it.body }
            }
            else -> return false
        }
        if (branches.isEmpty() || candidate.arguments.size != branches.size ||
            candidate.arguments.zip(branches).any { (atom, branch) -> atom.expression !== branch }
        ) return false
        val types = branches.map { it.coneTypeOrNull ?: return false }
        if (types.any { !isClosedSourceType(it) } || types.all {
                AbstractTypeChecker.isSubtypeOf(session.typeContext, it, target)
            }
        ) return false
        val resultType = candidate.substitutedReturnTypeFromCalculator() as? ConeTypeVariableType ?: return false
        val owned = candidate.freshVariables.singleOrNull { it.typeConstructor === resultType.typeConstructor } ?: return false
        return candidate.withConstraintSystemForCompletionResults { system ->
            if (isWithdrawn()) return@withConstraintSystemForCompletionResults false
            val storage = system.captureStorage()
            storage.allTypeVariables[owned.typeConstructor] === owned &&
                owned.typeConstructor !in storage.notFixedTypeVariables &&
                storage.fixedTypeVariables[owned.typeConstructor] is ConeErrorType &&
                storage.constraintsFromAllForkPoints.isEmpty() && storage.errors.isNotEmpty() &&
                storage.errors.all {
                    it is NotEnoughInformationForTypeParameter<*> &&
                        it.typeVariable === owned
                }
        }
    }

    /**
     * source 的明确目标失败只交给真实 tail 消费点；D 的 NotEnough-only 判定保持原合同。
     * 新通道只接受本 select K 的 Expected/Fix 初始约束，且每个实际错误都匹配同一 source target。
     * 不改 VM/固定解/失败引用，不接收 operator 借用目标或真实 generic/child 约束错误。
     */
    private fun <D> isOwnedTypedSelectTargetConstraintFailure(
        carrier: D,
        candidate: Candidate,
        target: ConeCangJieType?,
    ): Boolean where D : CfirExpression, D : CfirResolvable {
        fun withdrawn(): Boolean = candidate.isSourceCompletionWithdrawn ||
            candidate.implicitTypeComputationScope?.graph?.isCandidateWithdrawn(candidate) == true
        if (completionSystem == null || target == null || candidate.callInfo.callKind != CallKind.SyntheticSelect ||
            candidate.callInfo.callSite !== carrier ||
            (carrier.calleeReference as? CfirNamedReferenceWithCandidate)?.candidate !== candidate || withdrawn()
        ) return false
        val sourceMode = candidate.callInfo.resolutionMode as? ResolutionMode.WithExpectedType ?: return false
        if (sourceMode.fromCast || sourceMode.isOperatorOperandInference) return false
        fun closed(type: ConeCangJieType): Boolean {
            var result = true
            type.forEachTypeIncludingMetadata {
                if (it is ConeTypeVariableType || it is ConeStubType || it is ConeErrorType ||
                    it is ConeIdealLiteralType || it is ConePrimitiveType && it.kind.isIdeal
                ) result = false
            }
            return result
        }
        if (!closed(target) || !closed(sourceMode.expectedType) ||
            !AbstractTypeChecker.equalTypes(session.typeContext, sourceMode.expectedType, target)
        ) return false
        val branches = when (carrier) {
            is CfirIfExpression -> listOf(carrier.thenBranch, carrier.elseBranch ?: return false)
            is CfirMatchExpression -> carrier.branches.map { it.body }
            is CfirTryExpression -> {
                if (carrier.resources.isNotEmpty() || carrier.handlers.isNotEmpty()) return false
                listOf(carrier.tryBlock) + carrier.catches.map { it.body }
            }
            else -> return false
        }
        // 返回 Boolean 的本地只读遍历在撤权点立即停止；共享 completion traversal 不改变。
        val pendingInputs = ArrayDeque<ConeResolutionAtom>()
        val visitedAtoms = Collections.newSetFromMap(IdentityHashMap<ConeResolutionAtom, Boolean>())
        val visitedCandidates = Collections.newSetFromMap(IdentityHashMap<Candidate, Boolean>())
        fun queueActiveCandidate(child: Candidate): Boolean {
            if (child.isSourceCompletionWithdrawn ||
                child.implicitTypeComputationScope?.graph?.isCandidateWithdrawn(child) == true
            ) return false
            if (!visitedCandidates.add(child)) return true
            child.dispatchReceiver?.let(pendingInputs::addLast)
            child.givenExtensionReceiver?.let(pendingInputs::addLast)
            child.chosenExtensionReceiver?.let(pendingInputs::addLast)
            child.contextArguments?.let(pendingInputs::addAll)
            pendingInputs.addAll(child.arguments)
            pendingInputs.addAll(child.postponedAtoms)
            pendingInputs.addAll(child.postponedPCLACalls)
            return true
        }
        if (!queueActiveCandidate(candidate)) return false
        while (pendingInputs.isNotEmpty()) {
            val atom = pendingInputs.removeLast()
            if (!visitedAtoms.add(atom)) continue
            when (atom) {
                is ConeAtomWithCandidate -> if (!queueActiveCandidate(atom.candidate)) return false
                is ConeResolvedLambdaAtom -> {
                    if (!queueActiveCandidate(atom.candidateOfOuterCall)) return false
                    if (atom.analyzed) pendingInputs.addAll(atom.returnStatements)
                }
                is ConeLambdaWithTypeVariableAsExpectedTypeAtom -> {
                    if (!queueActiveCandidate(atom.candidateOfOuterCall)) return false
                    atom.subAtom?.let(pendingInputs::addLast)
                }
                is ConeResolvedCallableReferenceAtom -> {
                    if (!queueActiveCandidate(atom.candidateOfOuterCall)) return false
                    (atom.resultingReference as? CfirNamedReferenceWithCandidate)?.candidate?.let {
                        if (!queueActiveCandidate(it)) return false
                    }
                }
                is ConeSimpleNameForContextSensitiveResolution -> {
                    if (!queueActiveCandidate(atom.containingCallCandidate)) return false
                }
                is ConeResolutionAtomWithSingleChild -> atom.subAtom?.let(pendingInputs::addLast)
                is ConeResolutionAtomWithPostponedChild -> atom.subAtom?.let(pendingInputs::addLast)
                is ConeContextSensitiveAlternativeForQualifierAtom, is ConeSimpleLeafResolutionAtom -> Unit
            }
            val actualOwner = ((atom.expression as? CfirResolvable)?.calleeReference as? CfirNamedReferenceWithCandidate)?.candidate
            if (actualOwner != null && !queueActiveCandidate(actualOwner)) return false
        }
        if (branches.isEmpty() || branches.size != candidate.arguments.size ||
            branches.zip(candidate.arguments).any { (branch, atom) -> branch !== atom.expression }
        ) return false
        val types = branches.map { it.coneTypeOrNull ?: return false }
        if (types.any { !closed(it) }) return false
        val mismatches = types.filterNot { AbstractTypeChecker.isSubtypeOf(session.typeContext, it, target) }
        if (mismatches.isEmpty()) return false
        val signature = candidate.substitutedReturnTypeFromCalculator() as? ConeTypeVariableType ?: return false
        val owned = candidate.freshVariables.singleOrNull { it.typeConstructor === signature.typeConstructor } ?: return false
        fun root(type: ConeCangJieType): Boolean {
            if (type !is ConeTypeVariableType || type.typeConstructor !== owned.typeConstructor) return false
            var valid = true
            type.forEachTypeIncludingMetadata {
                if (it is ConeTypeVariableType && it.typeConstructor !== owned.typeConstructor ||
                    it is ConeStubType || it is ConeErrorType || it is ConeIdealLiteralType ||
                    it is ConePrimitiveType && it.kind.isIdeal
                ) valid = false
            }
            return valid
        }
        fun expected(type: ConeCangJieType): Boolean = closed(type) &&
            AbstractTypeChecker.equalTypes(session.typeContext, type, target)
        if (!root(signature)) return false
        return candidate.withConstraintSystemForCompletionResults { system ->
            if (withdrawn()) return@withConstraintSystemForCompletionResults false
            val storage = system.captureStorage()
            val fixed = storage.fixedTypeVariables[owned.typeConstructor]?.asCone()
            if (storage.allTypeVariables[owned.typeConstructor] !== owned ||
                owned.typeConstructor in storage.notFixedTypeVariables || fixed == null || !expected(fixed) ||
                storage.constraintsFromAllForkPoints.isNotEmpty() || storage.errors.isEmpty()
            ) return@withConstraintSystemForCompletionResults false
            var expectedOrigin = false
            val ownedErrors = storage.errors.all { error ->
                if (error !is ConstraintError) return@all false
                val lower = error.lowerType.asCone()
                val upper = error.upperType.asCone()
                if (!closed(lower) || !expected(upper) ||
                    mismatches.none { AbstractTypeChecker.equalTypes(session.typeContext, it, lower) }
                ) return@all false
                val initial = error.position.initialConstraint
                val a = initial.a.asCone()
                val b = initial.b.asCone()
                val sourcePair = when (initial.constraintKind) {
                    ConstraintKind.LOWER -> expected(a) && root(b)
                    ConstraintKind.UPPER -> root(a) && expected(b)
                    ConstraintKind.EQUALITY -> root(a) && expected(b) || expected(a) && root(b)
                }
                if (!sourcePair) return@all false
                when (val position = error.position.from) {
                    ConeExpectedTypeConstraintPosition -> { expectedOrigin = true; true }
                    is ConeFixVariableConstraintPosition -> position.variable === owned &&
                        initial.constraintKind == ConstraintKind.EQUALITY
                    else -> false
                }
            }
            ownedErrors && expectedOrigin
        }
    }

    /** 借用的 operator target 不能收窄实际 FULL 产生的 proper 值；不重新 Join 或固定变量。 */
    private fun preservesActualOperatorSourceType(
        candidate: Candidate,
        actualType: ConeCangJieType,
        target: ConeCangJieType,
    ): Boolean {
        if (candidate.isSourceCompletionWithdrawn ||
            candidate.implicitTypeComputationScope?.graph?.isCandidateWithdrawn(candidate) == true ||
            !actualType.isClosedOperatorInputType() || !target.isClosedOperatorInputType() ||
            AbstractTypeChecker.isSubtypeOf(session.typeContext, actualType, target)
        ) return false
        val signature = candidate.substitutedReturnTypeFromCalculator() as? ConeTypeVariableType ?: return false
        val owned = candidate.freshVariables.singleOrNull { it.typeConstructor === signature.typeConstructor } ?: return false
        var ownsSignature = true
        signature.forEachTypeIncludingMetadata {
            if (it is ConeTypeVariableType && it.typeConstructor !== owned.typeConstructor ||
                it is ConeStubType || it is ConeErrorType || it is ConeIdealLiteralType ||
                it is ConePrimitiveType && it.kind.isIdeal
            ) ownsSignature = false
        }
        if (!ownsSignature) return false
        val storage = completionSystem?.captureStorage() ?: return false
        if (storage.allTypeVariables[owned.typeConstructor] !== owned) return false
        val fixed = storage.fixedTypeVariables[owned.typeConstructor]?.asCone() ?: return false
        return fixed.isClosedOperatorInputType() && AbstractTypeChecker.equalTypes(session.typeContext, fixed, actualType)
    }

    /** 条件、selector、pattern、guard 不继承值分支的 expected type；finally 不参加 select。 */
    private fun <D> transformSyntheticCallChildren(syntheticCall: D, data: ExpectedArgumentType?)
            where D : CfirResolvable, D : CfirExpression {
        syntheticCall.transformAnnotations(this, null)
        syntheticCall.transformCalleeReference(this, null)
        when (syntheticCall) {
            is CfirIfExpression -> {
                syntheticCall.transformCondition(this, null)
                syntheticCall.transformThenBranch(this, data)
                syntheticCall.transformElseBranch(this, data)
            }
            is CfirMatchExpression -> {
                syntheticCall.transformSubject(this, null)
                for (branch in syntheticCall.branches) {
                    val previousError = (branch.coneTypeOrNull as? ConeErrorType)
                        ?.takeUnless { it.diagnostic.unwrapUnreportedDuplicateDiagnostic() === ConePostponedInferenceDiagnostic }
                    branch.transformAnnotations(this, null)
                    branch.transformPattern(this, null)
                    branch.transformGuard(this, null)
                    branch.transformBody(this, data)
                    branch.replaceConeTypeOrNull(
                        previousError ?: branch.body.firstErrorTypeOrNull(session) ?: checkNotNull(branch.body.coneTypeOrNull),
                    )
                }
            }
            is CfirTryExpression -> {
                // 资源/effect try 仍由其已确认的专用 resolver 完成，不向内部 body 注入普通结果目标。
                val branchData = if (syntheticCall.resources.isEmpty() && syntheticCall.handlers.isEmpty()) data else null
                syntheticCall.transformTryBlock(this, branchData)
                for (catchClause in syntheticCall.catches) {
                    val previousError = (catchClause.coneTypeOrNull as? ConeErrorType)
                        ?.takeUnless { it.diagnostic.unwrapUnreportedDuplicateDiagnostic() === ConePostponedInferenceDiagnostic }
                    catchClause.transformAnnotations(this, null)
                    catchClause.transformPattern(this, null)
                    catchClause.transformBody(this, branchData)
                    catchClause.replaceConeTypeOrNull(
                        previousError ?: catchClause.body.firstErrorTypeOrNull(session) ?: checkNotNull(catchClause.body.coneTypeOrNull),
                    )
                }
            }
            else -> error("Unexpected synthetic select carrier: " + syntheticCall.javaClass.name)
        }
    }

    /** 官方 MatchCase 可见 Join；pattern/guard 错误及非穷尽状态仍保留原 source owner。 */
    private fun completedMatchResultType(
        matchExpression: CfirMatchExpression,
        checkedTarget: ConeCangJieType?,
    ): ConeCangJieType {
        if (matchExpression.exhaustiveness is CfirMatchExhaustivenessStatus.NonExhaustive) {
            return ConeErrorType(ConeUnreportedDuplicateDiagnostic(ConeSimpleDiagnostic("Non-exhaustive match expression")))
        }
        val types = matchExpression.branches.map { checkNotNull(it.coneTypeOrNull) }
        val sourceError = (sequenceOf(matchExpression.subject?.coneTypeOrNull as? ConeErrorType) +
            types.asSequence().map { it as? ConeErrorType }).firstSourceErrorOrPending()
        if (sourceError != null) return ConeErrorType(ConeUnreportedDuplicateDiagnostic(sourceError.diagnostic))
        return completedVisibleBranchResult(types, checkedTarget, "MatchCase")
    }

    /** 官方 try/catch 按源码顺序 Join，catch 失败不覆盖前序已完成结果。 */
    private fun completedTryResultType(
        tryExpression: CfirTryExpression,
        checkedTarget: ConeCangJieType?,
    ): ConeCangJieType {
        var sourceError = tryExpression.tryBlock.firstErrorTypeOrNull(session)
        var joined = if (sourceError == null) checkNotNull(tryExpression.tryBlock.coneTypeOrNull)
            else session.builtinTypes.nothingType
        var firstJoinError: ConeErrorType? = null
        for (catchClause in tryExpression.catches) {
            val bodyError = sequenceOf(
                catchClause.coneTypeOrNull as? ConeErrorType,
                catchClause.body.firstErrorTypeOrNull(session),
            ).firstSourceErrorOrPending()
            if (bodyError != null) {
                sourceError = sequenceOf(sourceError, bodyError).firstSourceErrorOrPending()
                continue
            }
            val catchType = checkNotNull(catchClause.body.coneTypeOrNull)
            if (checkedTarget == null) {
                val next = session.typeContext.commonVisibleSuperTypeOrNull(listOf(joined, catchType))
                if (next == null) {
                    // 官方按序诊断每个失败 catch；失败不覆盖前序 Join，也不跳过后续 catch。
                    val error = ConeErrorType(ConeMismatchingCatchBlockError(catchType, joined))
                    catchClause.replaceConeTypeOrNull(error)
                    if (firstJoinError == null) firstJoinError = error
                } else {
                    joined = next
                }
            }
        }
        val error = sequenceOf(sourceError, firstJoinError).firstSourceErrorOrPending()
        if (error != null) return ConeErrorType(ConeUnreportedDuplicateDiagnostic(error.diagnostic))
        return checkedTarget ?: joined
    }

    /** 确定 target 来自正式消费点；无 target 时只发布 source-visible 最小共同父型。 */
    private fun completedVisibleBranchResult(
        branchTypes: List<ConeCangJieType>,
        checkedTarget: ConeCangJieType?,
        constructName: String,
    ): ConeCangJieType {
        branchTypes.asSequence().map { it as? ConeErrorType }.firstSourceErrorOrPending()?.let {
            check(it.diagnostic.unwrapUnreportedDuplicateDiagnostic() !== ConePostponedInferenceDiagnostic) { "FULL select contains an unfinished branch" }
            return ConeErrorType(ConeUnreportedDuplicateDiagnostic(it.diagnostic))
        }
        if (branchTypes.isEmpty()) return session.builtinTypes.unitType
        val types = branchTypes.map { IdealTypeResolver.resolveIfIdeal(it, checkedTarget) }
        return checkedTarget ?: session.typeContext.commonVisibleSuperTypeOrNull(types)
            ?: ConeErrorType(ConeIncompatibleExpressionTypesError(constructName, types))
    }

    /**
     * 官方 ChkIfExpr/SynIfExpr 的完成结果：只有真实分支写回后才执行可见 Join。
     * 没有结果的分支由正式 Nothing 表达；缺失类型不是 Unit 或 Any 的来源。
     */
    private fun completedIfResultType(
        ifExpression: CfirIfExpression,
        checkedTarget: ConeCangJieType?,
    ): ConeCangJieType {
        val branchError = sequenceOf(
            ifExpression.thenBranch.firstErrorTypeOrNull(session),
            checkNotNull(ifExpression.elseBranch).firstErrorTypeOrNull(session),
            ifExpression.condition.coneTypeOrNull as? ConeErrorType,
        ).firstSourceErrorOrPending()
        if (branchError != null) return ConeErrorType(ConeUnreportedDuplicateDiagnostic(branchError.diagnostic))
        val thenType = checkNotNull(ifExpression.thenBranch.coneTypeOrNull) { "Completed if then branch has no type" }
        val elseType = checkNotNull(checkNotNull(ifExpression.elseBranch).coneTypeOrNull) {
            "Completed if else branch has no type"
        }
        if (thenType.isNothing && elseType.isNothing) return session.builtinTypes.nothingType
        return completedVisibleBranchResult(
            if (checkedTarget == null) listOf(thenType.approximateThisTypeForDeclaration(), elseType.approximateThisTypeForDeclaration())
            else listOf(thenType, elseType),
            checkedTarget,
            "if expression",
        )
    }

    /**
     * 对位 Kotlin completion writer 的字面量近似；暂定 writer 不提前默认化尚未完成的分支。
     * 已固定 selector 和显式后缀类型由原近似器保持，期望类型只约束仍为 ideal 的字面量。
     */
    override fun transformLiteralExpression(
        literalExpression: CfirLiteralExpression,
        data: ExpectedArgumentType?,
    ): CfirExpression {
        val expectedType = data?.getExpectedType(literalExpression)?.let(::finallySubstituteOrSelf)
        if (expectedType?.contains { it is ConeTypeVariableType || it is ConeErrorType } == true) return literalExpression
        if (expectedType == null && completionSystem == null) return literalExpression
        return literalExpression.transformSingle(integerOperatorApproximator, expectedType)
    }

    /**
     * 完成 block 的真实尾表达式并重算结果类型；非尾语句保持各自按节点索引的完成上下文。
     * 分支目标不能传播到之前的初始化器、独立 lambda 或声明体。
     */
    override fun transformBlock(block: CfirBlock, data: ExpectedArgumentType?): CfirExpression {
        val expectedType = data?.getExpectedType(block)
        val resultExpression = block.statements.lastOrNull() as? CfirExpression
        val childrenData = if (expectedType != null && resultExpression != null) {
            when (data) {
                is ExpectedArgumentType.ArgumentsMap -> ExpectedArgumentType.ArgumentsMap(
                    map = data.map + (resultExpression to expectedType),
                    lambdasReturnTypes = data.lambdasReturnTypes,
                    forErrorReference = data.forErrorReference,
                    argumentMappingFailed = data.argumentMappingFailed,
                    argumentReplacements = data.argumentReplacements,
                )
                is ExpectedArgumentType.ExpectedType -> ExpectedArgumentType.ArgumentsMap(
                    map = mapOf(resultExpression to expectedType),
                    lambdasReturnTypes = emptyMap(),
                    forErrorReference = false,
                    argumentMappingFailed = data.argumentMappingFailed,
                    argumentReplacements = data.argumentReplacements,
                )
            }
        } else data

        block.transformChildren(this, childrenData)
        block.writeResultType(session)
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

    /** 已检查接收者的 payload 类型与原 Option 接收者不同，写回只能替换其中的类型变量。 */
    override fun transformOptionalExpression(
        optionalExpression: CfirOptionalExpression,
        data: ExpectedArgumentType?,
    ): CfirExpression = completeOptionalType(optionalExpression, data)

    /** 链结果已经包含规定的 Option 层数或 Unit；外部目标不能再次传进 selector。 */
    override fun transformOptionalChainExpression(
        optionalChainExpression: CfirOptionalChainExpression,
        data: ExpectedArgumentType?,
    ): CfirExpression = completeOptionalType(optionalChainExpression, data)

    private fun completeOptionalType(expression: CfirWrappedExpression, data: ExpectedArgumentType?): CfirExpression {
        val resolvedType = expression.coneTypeOrNull
        // 参数映射失败时，这里可能收到尚未分析的可选链。保留其空类型状态，同时把失败
        // 标记传给内嵌 lambda；单一外部目标不得传入 selector，按节点索引的既有完成信息仍保留。
        val childData = when (data) {
            is ExpectedArgumentType.ExpectedType -> ExpectedArgumentType.ArgumentsMap(
                map = emptyMap(),
                lambdasReturnTypes = emptyMap(),
                forErrorReference = false,
                argumentMappingFailed = data.argumentMappingFailed,
                argumentReplacements = data.argumentReplacements,
            )
            else -> data
        }
        expression.transformChildren(this, childData)
        expression.replaceConeTypeOrNull(resolvedType?.let(::finallySubstituteOrSelf))
        return expression
    }

    /**
     * 写回 range 表达式及其端点期望类型。
     */
    override fun transformRangeExpression(rangeExpression: CfirRangeExpression, data: ExpectedArgumentType?): CfirExpression {
        data?.argumentReplacements?.get(rangeExpression)?.let { replacement ->
            return replacement.transformSingle(this, data)
        }

        val expectedRangeType = data?.getExpectedType(rangeExpression)?.rangeTypeForExpectedType(session)
        val endpointExpectedType = expectedRangeType?.typeArguments?.singleOrNull()?.type
        val endpointData = endpointExpectedType?.toExpectedType(
            argumentReplacements = data.argumentReplacements,
            argumentMappingFailed = data.argumentMappingFailed == true,
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
     * 写回二元表达式，并按 completion 阶段才可得的实参期望类型重估 `??` 的右操作数分支。
     *
     * body resolve 阶段调用实参还没有形参期望类型（与 K2 一致：实参期望类型由 call completion
     * 阶段下发），`ChkCoalescingExpr` 的 `realTgtTy = tgtTy ?: 元素类型` 在那里只能退化为元素类型，
     * 可能记下 `ConeCoalescingRightOperandMismatch` 标记。completion 拿到 `ExpectedArgumentType`
     * 后按同一判据（`isDefiniteCoalescingRightMismatch`）重估：仍不匹配则把标记目标更新为当前
     * 目标类型；匹配则摘除标记、恢复为该目标类型（如 `test(v ?? "fail")`，`v: ?Int64`、形参
     * `ToString`，官方 0 诊断）。非实参位置没有期望类型可下发，保持 body resolve 的判定。
     */
    override fun transformBinaryOp(binaryOp: CfirBinaryOp, data: ExpectedArgumentType?): CfirExpression {
        data?.argumentReplacements?.get(binaryOp)?.let { replacement ->
            return replacement.transformSingle(this, data)
        }
        if (binaryOp.kind == CfirBinaryOpKind.COALESCING) {
            reevaluateCoalescingRightOperandMarker(binaryOp, data?.getExpectedType(binaryOp))
        }
        binaryOp.transformChildren(this, data)
        return binaryOp
    }

    private fun reevaluateCoalescingRightOperandMarker(binaryOp: CfirBinaryOp, expectedType: ConeCangJieType?) {
        if (expectedType == null) return
        val leftElementType = binaryOp.left.coneTypeOrNull?.optionElementType ?: return
        val targetType = coalescingTargetType(leftElementType, expectedType, session)
        val rightType = binaryOp.right.coneTypeOrNull
        if (rightType != null && isDefiniteCoalescingRightMismatch(rightType, targetType, session)) {
            binaryOp.replaceConeTypeOrNull(
                ConeErrorType(
                    ConeCoalescingRightOperandMismatch("coalescing right operand type mismatch", targetType),
                    delegatedType = targetType,
                ),
            )
            return
        }
        val currentMarker = (binaryOp.coneTypeOrNull as? ConeErrorType)?.diagnostic
            as? ConeCoalescingRightOperandMismatch ?: return
        binaryOp.replaceConeTypeOrNull(targetType)
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
     * 补全阶段把已选候选的函数形参类型写回省略标注的 lambda 参数。
     *
     * Kotlin FIR 在 `FirCallCompleter.LambdaAnalyzerImpl` 中完成这一步；仓颉的
     * overload-by-lambda 会在候选回滚后再由 results writer 统一落树，因此这里
     * 对同一份 expected function type 做最终写回；显式参数保留官方 ChkFuncParam 的源码类型。
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

        val parameterTypingFailure = anonymousFunction.lambdaParameterTypingFailure(session, expectedFunctionType)
        anonymousFunction.valueParameters.forEachIndexed { index, parameter ->
            // 与首次body分析保持同一契约，补全不能用目标函数类型覆盖源码显式标注。
            if (!parameter.hasOmittedLambdaParameterType()) return@forEachIndexed
            if (parameterTypingFailure != null && index >= parameterTypingFailure.firstUncheckedParameterIndex) {
                parameter.replaceReturnTypeRef(
                    parameter.returnTypeRef.resolvedTypeFromPrototype(
                        parameterTypingFailure.parameterErrorType, parameter.returnTypeRef.source,
                    ),
                )
                return@forEachIndexed
            }
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
        // enum owner 的完成替换只进入候选与返回类型，非法的成员实参仍保留源码类型供诊断与导航。
        if (with(CfirMapTypeArguments) { candidate.isQualifiedEnumMemberWithTypeArguments() }) {
            return access.typeArguments.filterIsInstance<CfirResolvedTypeRef>()
        }
        /*
         * `CPointer<T>(...)` is represented by a synthetic callable only so that
         * the ordinary argument stages can infer T.  The inferred T is not a
         * source type argument and must not be written back into the call tree:
         * official PointerExpr diagnostics render an omitted type argument as
         * `CPointer(...)`.  Explicit source arguments remain in `access` and
         * are appended below, so this branch changes only the synthetic
         * inference artifact.
         */
        if (
            candidate.symbol.cfir is CfirCallableDeclaration &&
                candidate.symbol.cfir.origin == CfirDeclarationOrigin.Synthetic.BuiltinPointerConstructor &&
                !candidate.callInfo.hasExplicitTypeArguments
        ) {
            return access.typeArguments.filterIsInstance<CfirResolvedTypeRef>()
        }

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

    /** 凭证由实际 completer factory 创建，内部绑定不扩散 internal 图类型到 public constructor。 */
    private val completionLeases = java.util.IdentityHashMap<CfirImplicitTypeComputation, CfirRetainedCompletionLease>()
    private val completionCandidates = java.util.IdentityHashMap<CfirElement, Candidate>()
    private val completionArgumentReplacements = java.util.IdentityHashMap<CfirElement, Pair<Candidate, CfirExpression>>()
    private var completionAuthorityBound = false
    /** FULL factory 的 owner 代次；不从 completionSystem 非空或表达式 resolved 推导权限。 */
    private val unscopedCompletionPasses = java.util.IdentityHashMap<Candidate, CfirUnscopedCompletionPass>()
    private val unscopedCompletionEntries = ArrayDeque<CfirUnscopedCompletionEntry>()
    private val unscopedCandidateWriterEpochs = java.util.IdentityHashMap<Candidate, Any>()

    /** collector 只登记实际 owner 清单；这一步没有完成权限或可供消费的 receipt。 */
    private val unscopedCompletionOwners = mutableListOf<Candidate>()
    internal fun bindUnscopedCompletionOwners(owners: List<Candidate>) {
        check(completionAuthorityBound)
        unscopedCompletionOwners.addAll(owners)
    }

    internal fun activateUnscopedFullCompletion(authority: CfirFullCompletionAuthority) {
        check(completionAuthorityBound && completionSystem === authority.system && mode == Mode.Normal)
        check(sourceCompletionRootCandidate === authority.candidate && completionCandidates[authority.root] === authority.candidate)
        check(!authority.candidate.isSourceCompletionWithdrawn &&
            authority.generation === authority.candidate.unscopedCompletionGeneration &&
            authority.writerEpoch === authority.candidate.unscopedWriterEpoch)
        check(unscopedCompletionPasses.isEmpty())
        // 只有确有 unscoped 队列义务的 owner 需要此额外 pass；纯 scoped FORMAL 只受原 lease/binding 控制。
        unscopedCompletionOwners.filter { owner ->
            !owner.isSourceCompletionWithdrawn &&
                owner.postponedPCLACalls.filterIsInstance<ConeAtomWithCandidate>().any { atom ->
                    !atom.candidate.isSourceCompletionWithdrawn &&
                        atom.candidate.implicitTypeComputationScope == null && owner.pclaCallWriters[atom] == null
                }
        }.forEach { owner ->
            unscopedCompletionPasses[owner] = owner.captureUnscopedCompletionPass(authority)
            owner.postponedPCLACalls.filterIsInstance<ConeAtomWithCandidate>().filter { atom ->
                !atom.candidate.isSourceCompletionWithdrawn &&
                    atom.candidate.implicitTypeComputationScope == null && owner.pclaCallWriters[atom] == null
            }.forEach { atom ->
                // 正式 factory 边界正常初始化/同步实际 child；held writer 不再承担下一轮语义 rebase。
                atom.candidate.system
                unscopedCandidateWriterEpochs[atom.candidate] = atom.candidate.unscopedWriterEpoch
            }
        }
    }

    private fun withUnscopedCompletionEntry(owner: Candidate, atom: ConeAtomWithCandidate, write: () -> Unit) {
        if (owner.isSourceCompletionWithdrawn || atom.candidate.isSourceCompletionWithdrawn) return
        val pass = unscopedCompletionPasses[owner]
        if (pass == null) {
            write()
            return
        }
        if (!owner.isUnscopedCompletionPassActive(pass) || !mayWriteCompletion(owner)) return
        // 本轮 callback 可发布真正的新 candidate；已有 candidate 的撤销代次不能被刷新。
        unscopedCandidateWriterEpochs.putIfAbsent(atom.candidate, atom.candidate.unscopedWriterEpoch)
        if (unscopedCandidateWriterEpochs[atom.candidate] !== atom.candidate.unscopedWriterEpoch) return
        atom.candidate.withConstraintSystemForCompletionResults {
            val entry = CfirUnscopedCompletionEntry(pass, atom)
            check(entry.matchesCandidate())
            unscopedCompletionEntries.addLast(entry)
            try {
                write()
            } finally {
                check(unscopedCompletionEntries.removeLast() === entry)
            }
        }
    }

    /** 仅成功 FULL 的 queued call 返回点可发布 receipt，异常/部分写回不会执行这一点。 */
    private fun rememberUnscopedCompletion(candidate: Candidate, result: CfirExpression) {
        val system = completionSystem ?: return
        val entry = unscopedCompletionEntries.lastOrNull() ?: return
        if (entry.candidate !== candidate || entry.carrier !== result || entry.pass.system !== system ||
            !entry.pass.owner.isUnscopedCompletionPassActive(entry.pass) || !mayWriteCompletion(entry.pass.owner) ||
            !entry.matchesCandidate() || !candidate.isSuccessful || candidate.postponedAtoms.any { !it.analyzed }
        ) return
        val reference = (result as? CfirResolvable)?.calleeReference as? CfirResolvedNamedReference ?: return
        val type = result.coneTypeOrNull ?: return
        if (reference is CfirDiagnosticHolder || reference.resolvedSymbol !== entry.target ||
            type.contains { it is ConeTypeVariableType || it is ConeStubType || it is ConeErrorType }
        ) return
        if (result is CfirFunctionCall && candidate.callInfo.callKind == CallKind.Function &&
            !AbstractTypeChecker.equalTypes(session.typeContext,
                finalSubstitutor.substituteOrSelf(candidate.substitutedReturnTypeFromCalculator()), type)
        ) return
        val arguments = (result as? CfirFunctionCall)?.argumentList?.arguments.orEmpty()
        val access = result as? CfirQualifiedAccessExpression
        for (part in arguments + listOfNotNull(access?.explicitReceiver, access?.dispatchReceiver)) {
            val partType = part.coneTypeOrNull ?: return
            if (partType.contains { it is ConeTypeVariableType || it is ConeStubType || it is ConeErrorType }) return
        }
        for (argument in access?.typeArguments.orEmpty()) {
            val argumentType = argument.coneTypeOrNull ?: return
            if (argumentType.contains { it is ConeTypeVariableType || it is ConeStubType || it is ConeErrorType }) return
        }
        val storage = system.captureStorage()
        if (storage.hasContradiction || storage.notFixedTypeVariables.isNotEmpty() || storage.constraintsFromAllForkPoints.isNotEmpty()) return
        val childStorage = candidate.system.captureStorage()
        val owned = mutableListOf<ConeTypeVariable>()
        val seen = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<ConeTypeVariable, Boolean>())
        for (variable in candidate.freshVariables) if (seen.add(variable)) owned += variable
        for (constructor in candidate.additionalCompletionVariables) {
            val variable = childStorage.allTypeVariables[constructor] as? ConeTypeVariable ?: return
            if (seen.add(variable)) owned += variable
        }
        val actualSubstitutor = storage.buildCurrentSubstitutor(session.typeContext, emptyMap()).asCone()
        val bindings = mutableListOf<CfirUnscopedFixedBinding>()
        for (variable in owned) {
            if (storage.allTypeVariables[variable.typeConstructor] !== variable ||
                childStorage.allTypeVariables[variable.typeConstructor] !== variable
            ) return
            val fixed = storage.fixedTypeVariables[variable.typeConstructor]?.asCone() ?: return
            val expected = actualSubstitutor.substituteOrSelf(fixed)
            val written = finalSubstitutor.substituteOrSelf(variable.defaultType)
            if (expected.contains { it is ConeTypeVariableType || it is ConeStubType || it is ConeErrorType } ||
                written.contains { it is ConeTypeVariableType || it is ConeStubType || it is ConeErrorType } ||
                !AbstractTypeChecker.equalTypes(session.typeContext, expected, written)
            ) return
            childStorage.fixedTypeVariables[variable.typeConstructor]?.asCone()?.let { childFixed ->
                val childType = childStorage.buildCurrentSubstitutor(session.typeContext, emptyMap()).asCone().substituteOrSelf(childFixed)
                if (childType.contains { it is ConeTypeVariableType || it is ConeStubType || it is ConeErrorType } ||
                    !AbstractTypeChecker.equalTypes(session.typeContext, expected, childType)
                ) return
            }
            bindings += CfirUnscopedFixedBinding(variable, expected)
        }
        entry.pass.owner.publishUnscopedCompletionReceipt(CfirUnscopedCompletionReceipt(entry, reference, type, bindings))
    }

    /** 历史 receipt 只消耗旧 atom；新 AST candidate 仍经原 queued guard 与真正 writer 写回。 */
    private fun consumeCompletedUnscopedBinding(owner: Candidate, atom: ConeAtomWithCandidate): Boolean {
        val system = completionSystem ?: return false
        // 正式 carrier 的候选只能沿 graph lease/exact terminal 通道消费，不能由旧 unscoped receipt 跳过。
        if (completionCandidates[atom.expression]?.implicitTypeComputationScope != null) return false
        val pass = unscopedCompletionPasses[owner] ?: return false
        if (!owner.isUnscopedCompletionPassActive(pass)) return false
        val receipt = owner.unscopedCompletionReceipt(atom, system) ?: return false
        check(!receipt.matchesCurrentResult() || receipt.canConsume(owner, system)) {
            "Queued completion carrier changed its actual candidate"
        }
        if (!receipt.canConsume(owner, system)) return false
        owner.consumeUnscopedCompletionReceipt(atom, receipt)
        if (completionCandidates[atom.expression] === atom.candidate) completionCandidates.remove(atom.expression)
        return true
    }

    private fun releaseCompletedPreviousBinding(owner: Candidate, atom: ConeAtomWithCandidate) {
        val system = completionSystem ?: return
        val pass = unscopedCompletionPasses[owner] ?: return
        if (!owner.isUnscopedCompletionPassActive(pass)) return
        val previous = completionCandidates[atom.expression] ?: return
        if (previous === atom.candidate || previous.implicitTypeComputationScope != null ||
            !owner.hasActualUnscopedReplacement(atom.expression, previous, system)
        ) return
        val oldAtom = owner.postponedPCLACalls.filterIsInstance<ConeAtomWithCandidate>().singleOrNull { old ->
            old !== atom && old.expression === atom.expression && old.candidate === previous &&
                owner.unscopedCompletionReceipt(old, system) != null
        } ?: return
        consumeCompletedUnscopedBinding(owner, oldAtom)
    }

    /** Source 与其后的普通回调保持同一 FIFO；整个批次结束后才退役图。 */
    private class PendingSourceResults(
        val owner: Candidate,
        val callbacks: List<(ConeSubstitutor) -> Unit>,
        val snapshot: List<(ConeSubstitutor) -> Unit>,
        val lambdas: List<CfirDeclaration>,
    )
    private val pendingSourceResults = mutableListOf<PendingSourceResults>()
    private var sourceCompletionRootCandidate: Candidate? = null

    internal fun bindSourceCompletionRoot(candidate: Candidate?) { sourceCompletionRootCandidate = candidate }

    internal fun bindCompletionAuthority(
        leases: Map<CfirImplicitTypeComputation, CfirRetainedCompletionLease>,
        candidates: Map<CfirElement, Candidate>,
        argumentReplacements: Map<CfirElement, Pair<Candidate, CfirExpression>>,
    ) {
        check(!completionAuthorityBound)
        completionAuthorityBound = true
        completionLeases.putAll(leases)
        completionCandidates.putAll(candidates)
        completionArgumentReplacements.putAll(argumentReplacements)
    }

    /** accepted successor 先核验 owning Candidate 与自身 lease，再改 owning AST/CFG；不复活 predecessor。 */
    private fun replacementForCompletion(expression: CfirExpression, data: ExpectedArgumentType?): CfirExpression? {
        val binding = completionArgumentReplacements[expression]
        val supplied = data?.argumentReplacements?.get(expression) ?: replacementFor(expression)
        if (binding != null) {
            check(supplied == null || supplied === binding.second) {
                "Completion replacement differs from its actual owning Candidate"
            }
            if (!mayWriteCompletion(binding.first)) return expression
        }
        val replacement = binding?.second ?: supplied ?: return null
        // 未由实际 owner 接受的替换仍须先验证原 carrier；已撤权 writer 不能借 resolved successor 越过旧 lease。
        if (binding == null && !prepareCandidateExpressionForCompletion(expression)) return expression
        check(replacement !== expression) { "Completion replacement owns itself" }
        if (!prepareCandidateExpressionForCompletion(replacement)) return expression
        return replacement
    }

    /** factory 后出现的真实 AST candidate 也必须在修改节点前取得明确的 FORMAL 代次绑定。 */
    private fun prepareCandidateExpressionForCompletion(expression: CfirExpression): Boolean {
        val reference = (expression as? CfirResolvable)?.calleeReference
        val candidate = if (reference is CfirNamedReferenceWithCandidate) reference.candidate else completionCandidates[expression]
        if (candidate == null) return true
        if (!mayWriteCompletion(candidate)) return false
        candidate.implicitTypeComputationScope?.let { scope ->
            if (completionSystem != null && scope.mode == CfirImplicitTypeComputationMode.FORMAL) {
                scope.graph.selectCompletionCandidate(scope, candidate)
                scope.graph.bindFormalCompletionToLease(scope, candidate, checkNotNull(completionLeases[scope.graph]))
            }
        }
        return true
    }

    /** 在修改 AST/队列之前验证 writer 资格，而非仅在 terminal 登记时过滤旧 writer。 */
    private fun mayWriteCompletion(candidate: Candidate): Boolean {
        if (candidate.isSourceCompletionWithdrawn) return false
        if (unscopedCompletionPasses.any { (owner, pass) -> !owner.isUnscopedCompletionPassActive(pass) } ||
            unscopedCandidateWriterEpochs.any { (actual, generation) -> actual.unscopedWriterEpoch !== generation }
        ) return false
        unscopedCompletionEntries.lastOrNull()?.takeIf { it.candidate === candidate }?.let { entry ->
            if (!entry.pass.owner.isUnscopedCompletionPassActive(entry.pass) || !entry.matchesCandidate()) return false
        }
        val scope = candidate.implicitTypeComputationScope ?: return true
        if (scope.graph.isCandidateWithdrawn(candidate)) return false
        if (completionSystem == null) return true
        val lease = checkNotNull(completionLeases[scope.graph]) { "Final writer has no graph ownership lease" }
        val active = scope.graph.isCompletionLeaseActive(scope, candidate, lease)
        return active
    }

    /** PCLA atom 持有实际 Candidate；partial writer 已 resolved 的节点按该 owner 进入同一正式写回路径。 */
    private fun <T> restoreExplicitCompletionReference(expression: T)
        where T : CfirExpression, T : CfirResolvable {

        val candidate = completionCandidates[expression] ?: return
        if (!mayWriteCompletion(candidate)) return
        val reference = expression.calleeReference
        if (reference !is CfirResolvedNamedReference || reference is CfirDiagnosticHolder) return
        if (candidate.implicitTypeComputationScope == null) {
            check(reference.resolvedSymbol === candidate.symbol) {
                "PCLA completion atom differs from the actual resolved target"
            }
        }
        candidate.implicitTypeComputationScope?.let { scope ->
            val matchesTarget = scope.graph.matchesResolvedCompletionTarget(scope, candidate, reference.resolvedSymbol)
            check(matchesTarget) {
                "PCLA completion atom differs from the actual resolved target"
            }
            if (completionSystem != null && scope.mode == CfirImplicitTypeComputationMode.FORMAL &&
                scope.graph.isFormalCallWrittenByLease(scope, candidate, expression, checkNotNull(completionLeases[scope.graph]))
            ) return
        }
        expression.replaceCalleeReference(CfirNamedReferenceWithCandidate(reference.source, candidate.callInfo.name, candidate))
    }

    /** 所有实参、子调用和重算回调结束后才发布 terminal；partial writer 无该 capability。 */
    private fun recordFinalCompletion(candidate: Candidate, result: CfirExpression) {
        val system = completionSystem ?: return
        candidate.implicitTypeComputationScope?.let { scope ->
            val lease = checkNotNull(completionLeases[scope.graph])
            // candidate metadata 持续承担修改前的 lease 检查；已写回判断归当前 graph binding。
            scope.graph.recordCompletedCall(scope, candidate, result, system, finalSubstitutor, lease)
        }
        if (candidate === sourceCompletionRootCandidate) {
            // 根调用和全部子调用已有真实 final outcome，才完成 source；最后统一退役，避免
            // 同图的另一个 source slot 或当前 writer 仍在使用 lease 时被第一项提前撤销。
            val batches = pendingSourceResults.toList()
            pendingSourceResults.clear()
            val completedSources = mutableListOf<CfirResourceSourceCompletion>()
            for (batch in batches) {
                if (!mayWriteCompletion(batch.owner)) continue
                fun write(callback: (ConeSubstitutor) -> Unit) {
                    if (!mayWriteCompletion(batch.owner) || batch.callbacks.none { it === callback }) return
                    if (callback is CfirResourceSourceCompletion) {
                        if (callback.isOwnedBy(batch.owner)) {
                            callback.complete(system, finalSubstitutor, batch.owner)
                            if (callback.isCompleted) completedSources += callback
                        }
                    } else if (callback is CfirRetainedAdditionalResultsProducer) {
                        callback.complete(system, finalSubstitutor)
                    } else {
                        callback(finalSubstitutor)
                    }
                }
                batch.snapshot.forEach(::write)
                // 普通回调按捕获快照执行；本轮新增的真实 Source 槽仍必须在退休前完成。
                batch.callbacks.filterIsInstance<CfirResourceSourceCompletion>()
                    .filter { source -> batch.snapshot.none { it === source } }.forEach(::write)
                if (mayWriteCompletion(batch.owner)) writePCLALambdaResults(batch.lambdas)
            }
            val retiredGraphs = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<CfirImplicitTypeComputation, Boolean>())
            completedSources.filter { it.isCurrent }.forEach { source ->
                val sourceGraph = source.graph ?: return@forEach
                if (sourceGraph !in retiredGraphs && source.retireAfterSourceCompletion()) retiredGraphs += sourceGraph
            }
        }
        rememberUnscopedCompletion(candidate, result)
    }

    /**
     * 执行候选关联的 PCLA 完成后写回任务。
     */
    private fun runPCLARelatedTasksForCandidate(candidate: Candidate) {
        for (postponedCall in candidate.postponedPCLACalls.toList()) {
            if (candidate.postponedPCLACalls.none { it === postponedCall }) continue
            val childWriter = candidate.pclaCallWriters[postponedCall]
            val actualCandidate = if (childWriter == null && postponedCall is ConeAtomWithCandidate) {
                ((postponedCall.expression as? CfirResolvable)?.calleeReference as? CfirNamedReferenceWithCandidate)?.candidate
            } else null
            // 回调/重查可在 factory 后发布新引用；先按实际 AST 和有效 lease 选择，
            // 再撤销旧 atom、核验旧 binding，不能由队列顺序撤销当前 AST 实例。
            actualCandidate?.implicitTypeComputationScope?.let { scope ->
                if (completionSystem != null && mayWriteCompletion(actualCandidate)) {
                    scope.graph.selectCompletionCandidate(scope, actualCandidate)
                }
            }
            val queuedCandidate = (postponedCall as? ConeAtomWithCandidate)?.candidate
            if (childWriter == null && postponedCall is ConeAtomWithCandidate && queuedCandidate?.implicitTypeComputationScope == null) {
                if (consumeCompletedUnscopedBinding(candidate, postponedCall)) continue
                releaseCompletedPreviousBinding(candidate, postponedCall)
            }
            if (queuedCandidate?.isSourceCompletionWithdrawn == true ||
                queuedCandidate?.implicitTypeComputationScope?.graph?.isCandidateWithdrawn(queuedCandidate) == true
            ) {
                candidate.removePostponedCall(postponedCall)
                continue
            }
            if (childWriter == null) {
                // factory 后新增的实际 atom 也要绑定 carrier；现有 lease/generation guard 决定是否可写。
                if (postponedCall is ConeAtomWithCandidate) {
                    if (!mayWriteCompletion(postponedCall.candidate)) continue
                    if (actualCandidate != null && !mayWriteCompletion(actualCandidate)) continue
                    val previous = completionCandidates[postponedCall.expression]
                    check((actualCandidate == null || actualCandidate === postponedCall.candidate) &&
                        (previous == null || previous === postponedCall.candidate || !mayWriteCompletion(previous))
                    ) {
                        "Queued completion carrier changed its actual candidate"
                    }
                    completionCandidates[postponedCall.expression] = postponedCall.candidate
                    postponedCall.candidate.implicitTypeComputationScope?.let { scope ->
                        if (completionSystem != null && mayWriteCompletion(postponedCall.candidate)) {
                            scope.graph.selectCompletionCandidate(scope, postponedCall.candidate)
                            if (scope.mode == CfirImplicitTypeComputationMode.FORMAL) {
                                scope.graph.bindFormalCompletionToLease(scope, postponedCall.candidate,
                                    checkNotNull(completionLeases[scope.graph]))
                            }
                        }
                    }
                }
                if (postponedCall is ConeAtomWithCandidate && postponedCall.candidate.implicitTypeComputationScope == null) {
                    withUnscopedCompletionEntry(candidate, postponedCall) {
                        postponedCall.expression.transform<CfirElement, ExpectedArgumentType?>(this, null)
                    }
                } else postponedCall.expression.transform<CfirElement, ExpectedArgumentType?>(this, null)
            } else {
                childWriter(postponedCall, finalSubstitutor)
            }
        }

        writePCLAAdditionalResults(candidate.onPCLACompletionResultsWritingCallbacks, candidate.lambdasAnalyzedWithPCLA, candidate)
    }

    /** 子推断图复用正式 writer 算法，但使用自己所属的 lexical/DFA 上下文。 */
    internal fun writePCLAAdditionalResults(
        callbacks: List<(ConeSubstitutor) -> Unit>,
        lambdas: List<CfirDeclaration>,
        sourceOwner: Candidate? = null,
    ) {
        if (completionSystem != null && sourceOwner != null &&
            callbacks.filterIsInstance<CfirResourceSourceCompletion>().any { it.isOwnedBy(sourceOwner) }
        ) {
            pendingSourceResults += PendingSourceResults(sourceOwner, callbacks, callbacks.toList(), lambdas.toList())
            return
        }
        for (callback in callbacks.toList()) {
            if (callback is CfirResourceSourceCompletion || callbacks.none { it === callback }) continue
            if (callback is CfirRetainedAdditionalResultsProducer && completionSystem != null) {
                callback.complete(completionSystem, finalSubstitutor)
                continue
            }
            callback(finalSubstitutor)
        }
        // 普通回调也可以引入新的 Source；由同一根完成点写回，绝不从 provisional writer 发布。
        if (completionSystem != null && sourceOwner != null) {
            val sources = callbacks.filterIsInstance<CfirResourceSourceCompletion>().filter { it.isOwnedBy(sourceOwner) }
            if (sources.isNotEmpty()) {
                pendingSourceResults += PendingSourceResults(sourceOwner, callbacks, sources, lambdas.toList())
                return
            }
        }
        writePCLALambdaResults(lambdas)
    }

    /** Kotlin 的 lambda 类型收敛位于声明完成回调之后。 */
    private fun writePCLALambdaResults(lambdas: List<CfirDeclaration>) {
        val typeVariablesAfterPCLATransformer = CfirTypeVariablesAfterPCLATransformer(finalSubstitutor)
        for (lambda in lambdas.toList()) {
            lambda.transformSingle(typeVariablesAfterPCLATransformer, null)
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
        withCallCfgOwner(anonymousFunction) {
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
            }

            anonymousFunction.body?.let { body ->
                if (newData != null) {
                    body.transform<CfirElement, ExpectedArgumentType?>(this, newData)
                } else {
                    body.transform(this, null)
                }
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
        // SynLamExpr 必须保留自身 body/return 的 InvalidTy，外围 expected R 不能擦掉它。
        val returnTypes = returnExpressions.map { checkNotNull(it.expression.coneTypeOrNull) }
        val sourceError = (returnTypes.asSequence().map { it as? ConeErrorType } +
            sequenceOf(anonymousFunction.body?.firstErrorTypeOrNull(session))).firstSourceErrorOrPending()
        if (sourceError?.isPostponedInference == true) return sourceError
        if (sourceError != null) return ConeErrorType(
            // 正文拥有原诊断，lambda 返回类型只传播 InvalidTy，避免在整个 lambda 上重复上报。
            diagnostic = ConeUnreportedDuplicateDiagnostic(sourceError.diagnostic.unwrapUnreportedDuplicateDiagnostic()),
            isUninferredParameter = sourceError.isUninferredParameter,
            delegatedType = sourceError.delegatedType,
            typeArguments = sourceError.typeArguments,
            attributes = sourceError.attributes,
        )
        if (anonymousFunction.isLambda && expectedReturnType?.isUnit == true) return expectedReturnType
        val inferredReturnType = if (returnTypes.isEmpty()) {
            checkNotNull(anonymousFunction.body?.coneTypeOrNull)
        } else {
            session.typeContext.commonSuperTypeOrNull(returnTypes)
                ?: checkNotNull(anonymousFunction.body?.coneTypeOrNull)
        }
        if (inferredReturnType is ConeErrorType) return inferredReturnType
        val checkedReturnType = expectedReturnType?.takeUnless {
            it.contains { type -> type is ConeErrorType || type is ConeTypeVariableType }
        }
        return if (anonymousFunction.isLambda && checkedReturnType != null && !inferredReturnType.isUnit) {
            checkedReturnType
        } else inferredReturnType
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

        // BlockEnter/Exit 节点持有当前 block；完成器补写隐式 return 时保持 block 身份，
        // 通过共有语句槽更新尾表达式，避免 AST 被另一份 block 替换后留下旧 CFG 节点。
        val statements = currentBody.statements as? MutableList<CfirStatement>
            ?: error("CfirBlock statements must be mutable during call completion")
        statements[statements.lastIndex] = buildReturnExpression {
            source = (
                lastStatement.source
                    ?: currentBody.source
                    ?: this@addReturnToLastStatementIfNeeded.source
                )?.fakeElement(CjFakeSourceElementKind.ImplicitReturn.FromLastStatement)
            // return 自身终止控制流，值及其错误归 result 所有；与显式 return 使用相同类型。
            coneTypeOrNull = ConePrimitiveType.NOTHING
            target = CfirFunctionTarget(labelName = null, isLambda = this@addReturnToLastStatementIfNeeded.isLambda).also {
                it.bind(this@addReturnToLastStatementIfNeeded)
            }
            result = lastStatement
        }
    }

    /**
     * 将 return 表达式集合中的 postponed atom 替换为完成后的表达式。
     */
    private fun Collection<CfirDataFlowAnalyzer.CfirAnonymousFunctionReturnExpressionInfo>.replacePostponedAtomsInReturnExpressions(
        data: ExpectedArgumentType?,
    ): Collection<CfirDataFlowAnalyzer.CfirAnonymousFunctionReturnExpressionInfo> {
        return map { returnInfo ->
            val replacement = data?.argumentReplacements?.get(returnInfo.expression) ?: replacePostponedAtom(returnInfo.expression)
            if (replacement === returnInfo.expression) return@map returnInfo
            class ReturnExpressionReplacer : org.cangnova.cangjie.cfir.visitors.CfirTransformer<Nothing?>() {
                override fun <E : CfirElement> transformElement(element: E, data: Nothing?): E {
                    @Suppress("UNCHECKED_CAST")
                    return if (element === returnInfo.expression) {
                        handleArgumentReplacementInCfg(returnInfo.expression, replacement)
                        replacement as E
                    } else element
                }

                override fun transformReturnExpression(returnExpression: CfirReturnExpression, data: Nothing?): CfirStatement =
                    returnExpression.transformResult(this, data)

                override fun transformBlock(block: CfirBlock, data: Nothing?): CfirStatement {
                    // 对位 Kotlin transformStatementsIndexed：仅转换实际 block 的最后一个 owning slot。
                    val last = block.statements.lastOrNull() ?: return block
                    val updated = last.transformSingle(this, data)
                    if (updated !== last) {
                        val statements = block.statements as? MutableList<CfirStatement>
                            ?: error("CfirBlock statements must be mutable during return adoption")
                        statements[statements.lastIndex] = updated
                    }
                    return block
                }
            }
            returnInfo.copy(
                expression = replacement,
                containingStatement = returnInfo.containingStatement.transformSingle(ReturnExpressionReplacer(), null),
            )
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
