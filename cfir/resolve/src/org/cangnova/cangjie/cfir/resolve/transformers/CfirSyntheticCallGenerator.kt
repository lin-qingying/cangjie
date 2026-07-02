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

import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS
import org.cangnova.cangjie.cfir.declarations.builder.buildNamedFunction
import org.cangnova.cangjie.cfir.declarations.builder.buildValueParameter
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.declarations.lambdaParameterShapeExpectedFunctionType
import org.cangnova.cangjie.cfir.diagnostic.ConeInapplicableCandidateError
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
