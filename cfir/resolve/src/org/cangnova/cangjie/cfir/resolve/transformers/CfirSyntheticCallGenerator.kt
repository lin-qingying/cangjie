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

package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS
import org.cangnova.cangjie.cfir.declarations.builder.buildNamedFunction
import org.cangnova.cangjie.cfir.declarations.builder.buildValueParameter
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.declarations.lambdaParameterShapeExpectedFunctionType
import org.cangnova.cangjie.cfir.diagnostic.ArgumentTypeMismatch
import org.cangnova.cangjie.cfir.diagnostic.ConeInapplicableCandidateError
import org.cangnova.cangjie.cfir.diagnostics.CfirDiagnosticHolder
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildArgumentList
import org.cangnova.cangjie.cfir.expressions.builder.buildFunctionCall
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.CfirLocalLambdaInitializerInferenceData
import org.cangnova.cangjie.cfir.resolve.CfirResolutionSnapshot
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.body.CfirAbstractBodyResolveTransformer
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallInfo
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallKind
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CandidateFactory
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirNamedReferenceWithCandidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.createErrorReferenceWithExistingCandidate
import org.cangnova.cangjie.cfir.resolve.inference.inferenceComponents
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.CfirPCLAInferenceSession
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeAnyType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType
import org.cangnova.cangjie.cfir.types.asCone
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.contains
import org.cangnova.cangjie.cfir.resolve.localLambdaInitializerInferenceData
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.calls.inference.buildCurrentSubstitutor
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import org.cangnova.cangjie.source.CjSourceElement

/**
 * 生成只参与解析流水线的合成调用。
 *
 * 对齐 Kotlin FIR `FirSyntheticCallGenerator`：顶层 lambda 先被包装成
 * `accept(argument: expectedOrAny)` 形态，再复用普通候选、实参检查、
 * postponed lambda completion 与结果写回，而不是在 lambda checker 中放行。
 */
class CfirSyntheticCallGenerator(
    /** body resolve transformer 共享组件。 */
    private val components: CfirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
) {
    /** 当前解析 session。 */
    private val session get() = components.session

    /**
     * 用合成外层调用解析匿名函数表达式。
     *
     * 匿名函数会作为单个实参传入 synthetic accept 函数，使普通调用完成逻辑负责 expected type 与 lambda body。
     */
    fun resolveAnonymousFunctionExpressionWithSyntheticOuterCall(
        anonymousFunctionExpression: CfirAnonymousFunctionExpression,
        expectedTypeData: ResolutionMode.WithExpectedType?,
        context: ResolutionContext,
    ): CfirExpression {
        val argumentList = buildArgumentList {
            arguments += anonymousFunctionExpression
        }
        val parameterType = expectedTypeData?.expectedType
            ?.takeUnless { it.isUnitOrAny() }
            ?: ConeAnyType
        parameterType.functionTypeForLambdaShape()?.let { expectedFunctionType ->
            val anonymousFunction = anonymousFunctionExpression.anonymousFunction
            anonymousFunction.lambdaParameterShapeExpectedFunctionType = expectedFunctionType
            anonymousFunction.replaceMatchingParameterFunctionType(expectedFunctionType)
        }
        val reference = generateCalleeReferenceToFunctionWithSingleParameterOfSpecifiedType(
            callSite = anonymousFunctionExpression,
            argument = anonymousFunctionExpression,
            parameterType = parameterType,
            context = context,
        )
        val fakeCall = buildFunctionCall {
            source = anonymousFunctionExpression.source
            calleeReference = reference
            this.argumentList = argumentList
        }

        components.dataFlowAnalyzer.enterCallArguments(fakeCall, argumentList.arguments)
        components.dataFlowAnalyzer.enterAnonymousFunctionExpression(anonymousFunctionExpression)
        components.dataFlowAnalyzer.exitCallArguments()
        components.dataFlowAnalyzer.enterFunctionCall(fakeCall)

        val preBodyResolveSnapshot = CfirResolutionSnapshot.capture(anonymousFunctionExpression)
        val resultingCall = components.callCompleter.completeCall(fakeCall, ResolutionMode.ContextIndependent)
        (resultingCall.calleeReference as? CfirDiagnosticHolder)?.diagnostic
            ?.let { diagnostic ->
                anonymousFunctionExpression.restoreActualTypeAfterSyntheticTypeMismatch(diagnostic)
            }
        if (parameterType == ConeAnyType) {
            val inferenceData = CfirLocalLambdaInitializerInferenceData(
                reference.candidate.system.currentStorage(),
                anonymousFunctionExpression,
                reference.candidate.postponedPCLACalls.toList(),
                preBodyResolveSnapshot,
            )
            inferenceData.reanalyzeTopLevelLambdaBodyIfPossible(reference)

            reference.candidate.system.currentStorage().takeIf { storage ->
                storage.notFixedTypeVariables.isNotEmpty() ||
                        anonymousFunctionExpression.hasLocalLambdaPlaceholderFrom(storage)
            }?.let { storage ->
                inferenceData.constraintStorage = storage
                anonymousFunctionExpression.anonymousFunction.localLambdaInitializerInferenceData = inferenceData
            }
        }

        components.dataFlowAnalyzer.exitFunctionCall(resultingCall, callCompleted = true)
        return resultingCall.argumentList.arguments.single()
    }

    /**
     * synthetic call 因 lambda 与非函数 expected type 不匹配时，恢复 lambda 的真实推断类型。
     *
     * completion writer 会在错误候选上把整个 lambda 暂时写成 expected type；该类型不能
     * 作为字段/局部声明初始化器的实际类型。只接受当前 lambda 整体对应的单一
     * [ArgumentTypeMismatch]，并沿候选约束系统替换其 actual type，避免吞掉形参/返回体等
     * 其他 lambda 诊断或重复生成错误。
     */
    private fun CfirAnonymousFunctionExpression.restoreActualTypeAfterSyntheticTypeMismatch(
        diagnostic: ConeDiagnostic,
    ) {
        if (diagnostic !is ConeInapplicableCandidateError) return
        val candidate = diagnostic.candidate as? Candidate ?: return
        val mismatch = candidate.diagnostics.singleOrNull { candidateDiagnostic ->
            candidateDiagnostic is ArgumentTypeMismatch && candidateDiagnostic.argument === this
        } as? ArgumentTypeMismatch ?: return

        val substitutor = candidate.system.currentStorage()
            .buildCurrentSubstitutor(session.typeContext, emptyMap())
            .asCone()
        val actualType = substitutor.substituteOrSelf(mismatch.actualType)
        anonymousFunction.replaceTypeRef(
            actualType.toCfirResolvedTypeRef(anonymousFunction.typeRef.source, anonymousFunction.typeRef),
        )
    }

    /**
     * synthetic accept 首轮 completion 后，若 lambda 参数类型已经能从 body 约束中确定，
     * 立即恢复首轮解析前的 body 并重算一次，清除同一 body 内早期成员访问/操作符解析留下的旧错误。
     */
    private fun CfirLocalLambdaInitializerInferenceData.reanalyzeTopLevelLambdaBodyIfPossible(
        reference: CfirNamedReferenceWithCandidate,
    ) {
        val candidate = reference.candidate
        val storage = candidate.system.currentStorage()
        val substitutor = storage.buildCurrentSubstitutor(session.typeContext, emptyMap()).asCone()
        val applied = applyCompletionResult(
            variable = null,
            substitutor = substitutor,
            completedStorage = storage,
            restoreBodyResolveState = true,
        )
        if (!applied) return

        val expression = lambdaExpression
        val lambda = expression.anonymousFunction
        val pclaInferenceSession = CfirPCLAInferenceSession(candidate, session.inferenceComponents)
        components.context.withAnonymousFunctionTowerDataContext(lambda.symbol) {
            components.context.withInferenceSession(pclaInferenceSession) {
                components.transformer.declarationsTransformer.doTransformAnonymousFunctionBodyFromCallCompletion(
                    expression,
                    null,
                )
                // 官方对无期望类型的 lambda 字面量按 body 结果自推断返回类型；若重算后
                // 返回占位仍是未固定推断变量而 body 末表达式类型已确定，必须把该类型
                // 约束到占位上。否则占位变量会随函数值调用的结果类型泄漏给外层调用，
                // 使 `println(f(...))` 这类重载集合因实参为未固定变量而整体保持"全部
                // 适用"，最终误报 AMBIGUOUS_FUNCTION_CALL。
                constrainLambdaReturnPlaceholderFromBody(lambda, pclaInferenceSession)
            }
            pclaInferenceSession.applyResultsToMainCandidate()
        }
        components.context.dropContextForAnonymousFunction(lambda)
        val completedStorage = candidate.system.currentStorage()
        val completedSubstitutor = completedStorage.buildCurrentSubstitutor(session.typeContext, emptyMap()).asCone()
        applyCompletionResult(
            variable = null,
            substitutor = completedSubstitutor,
            completedStorage = completedStorage,
            restoreBodyResolveState = false,
        )
    }

    /**
     * body 重算后把已确定的末表达式类型约束到仍未固定的返回占位。
     *
     * 仅当返回类型仍是推断变量且末表达式类型本身确定（非推断变量、非 error）时才生效；
     * 其余情况保持原状，交由真实调用点的 expected type 或既有诊断路径处理。
     */
    private fun constrainLambdaReturnPlaceholderFromBody(
        lambda: org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction,
        pclaInferenceSession: CfirPCLAInferenceSession,
    ) {
        val returnPlaceholder = lambda.returnTypeRef.coneTypeOrNull as? ConeTypeVariableType ?: return
        val lastExpression = lambda.body?.statements?.lastOrNull() as? CfirExpression ?: return
        val bodyResultType = lastExpression.coneTypeOrNull ?: return
        if (bodyResultType is ConeTypeVariableType || bodyResultType is ConeErrorType) return
        pclaInferenceSession.addSubtypeConstraintIfCompatible(bodyResultType, returnPlaceholder)
    }

    /** 构造接受单个指定类型参数的合成函数候选引用。 */
    private fun generateCalleeReferenceToFunctionWithSingleParameterOfSpecifiedType(
        callSite: CfirExpression,
        argument: CfirExpression,
        parameterType: ConeCangJieType,
        context: ResolutionContext,
    ): CfirNamedReferenceWithCandidate {
        val name = SYNTHETIC_ACCEPT_SPECIFIC_TYPE_NAME
        val functionSymbol = CfirNamedFunctionSymbol(CallableId(name))
        val function = buildNamedFunction {
            source = callSite.source
            moduleData = session.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.FakeFunction
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = true
            dispatchReceiverType = null
            status = CfirDeclarationStatusImpl()
            returnTypeRef = buildResolvedTypeRef {
                source = callSite.source
                coneType = session.builtinTypes.unitType
            }
            valueParameters += buildSyntheticValueParameter(
                ownerSymbol = functionSymbol,
                parameterName = SYNTHETIC_ACCEPT_PARAMETER_NAME,
                parameterType = parameterType,
                source = argument.source,
            )
            symbol = functionSymbol
            this.name = name
            isMut = false
        }
        val callInfo = generateCallInfo(callSite, name, listOf(argument), context)
        val candidate = CandidateFactory(context, callInfo).createCandidate(
            callInfo = callInfo,
            symbol = function.symbol,
            originScope = null,
            explicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
        )
        val applicability = components.resolutionStageRunner.processCandidate(candidate, context)
        val source = callSite.source

        if (!candidate.isSuccessful) {
            return createErrorReferenceWithExistingCandidate(
                candidate = candidate,
                diagnostic = ConeInapplicableCandidateError(applicability, candidate),
                source = source,
                resolutionContext = context,
                resolutionStageRunner = components.resolutionStageRunner,
            )
        }

        return CfirNamedReferenceWithCandidate(source, name, candidate)
    }

    /**
     * synthetic lambda 完成后若函数类型仍含当前约束系统里的 placeholder，
     * 后续函数值调用必须继续持有该系统才能把实参约束导回 lambda 参数。
     */
    private fun CfirAnonymousFunctionExpression.hasLocalLambdaPlaceholderFrom(
        storage: ConstraintStorage,
    ): Boolean {
        val functionType = coneTypeOrNull ?: anonymousFunction.typeRef.coneTypeOrNull ?: return false
        return functionType.contains { type ->
            type is ConeTypeVariableType && type.typeConstructor in storage.allTypeVariables
        }
    }

    /** 构造合成 accept 函数的唯一值参数。 */
    private fun buildSyntheticValueParameter(
        ownerSymbol: CfirNamedFunctionSymbol,
        parameterName: Name,
        parameterType: ConeCangJieType,
        source: CjSourceElement?,
    ) = buildValueParameter {
        this.source = source
        moduleData = session.moduleData
        resolvePhase = CfirResolvePhase.BODY_RESOLVE
        origin = CfirDeclarationOrigin.Synthetic.FakeFunction
        attributes = CfirDeclarationAttributes.EMPTY
        isLocal = true
        dispatchReceiverType = null
        symbol = CfirValueParameterSymbol(CallableId(parameterName))
        containingDeclarationSymbol = ownerSymbol
        isNamed = false
        status = DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS
        returnTypeRef = buildResolvedTypeRef {
            this.source = source
            coneType = parameterType
        }
        name = parameterName
        defaultValue = null
    }

    /** 为合成 accept 函数调用构造 [CallInfo]。 */
    private fun generateCallInfo(
        callSite: CfirExpression,
        name: Name,
        arguments: List<CfirExpression>,
        context: ResolutionContext,
    ): CallInfo = CallInfo(
        callSite = callSite,
        callKind = CallKind.Function,
        name = name,
        explicitReceiver = null,
        arguments = arguments,
        isUsedAsGetClassReceiver = false,
        typeArguments = emptyList(),
        session = session,
        containingFile = components.file,
        containingDeclarations = components.containingDeclarations,
        resolutionMode = ResolutionMode.ContextIndependent,
    )

    /** 判断类型是否为 Unit 或 Any，作为 synthetic 参数类型时无需保留具体期望类型。 */
    private fun ConeCangJieType.isUnitOrAny(): Boolean =
        this == ConeAnyType || with(session.typeContext) { this@isUnitOrAny.isUnit() }

    /** 取得 lambda 头部诊断可使用的目标函数类型。 */
    private fun ConeCangJieType.functionTypeForLambdaShape(): ConeFunctionType? =
        this as? ConeFunctionType
            ?: fullyExpandedType(session) as? ConeFunctionType

    private companion object {
        val SYNTHETIC_ACCEPT_SPECIFIC_TYPE_NAME: Name = Name.special("<synthetic-accept-specific-type>")
        val SYNTHETIC_ACCEPT_PARAMETER_NAME: Name = Name.identifier("argument")
    }
}
