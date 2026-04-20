package org.cangnova.cangjie.analysis.low.level.api.cfir.api

import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCallOrigin
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallInfo
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallKind
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability

/**
 * low-level 调用查询的真实结果模型。
 *
 * 这一层直接承接 CFIR resolve 已经存在的 [CallInfo] / [Candidate] 语义，
 * 供 `analysis-api-cfir` 映射到公开 Analysis API 调用模型。
 */
data class LLCallInfo(
    val successfulCall: LLCall?,
    val calls: List<LLCall>,
)

/**
 * 单个候选调用的 low-level 语义视图。
 *
 * 这里保留主干真实语义锚点 [callInfo] / [candidate]，
 * 避免再次压扁成与 CFIR 主干脱节的私有中间层。
 */
data class LLCall(
    val callInfo: CallInfo,
    val candidate: Candidate,
    val kind: CallKind,
    val origin: CfirFunctionCallOrigin,
    val applicability: CandidateApplicability,
    val isImplicitInvoke: Boolean,
    val calleeName: Name?,
    val target: CfirCallableSymbol<*>?,
    val explicitReceiverType: ConeCangJieType?,
    val dispatchReceiverType: ConeCangJieType?,
    val extensionReceiverType: ConeCangJieType?,
    val contextArgumentTypes: List<ConeCangJieType?>,
    val argumentTypes: List<ConeCangJieType?>,
    val typeArguments: List<ConeCangJieType?>,
    val argumentMapping: List<LLCallArgumentMapping>,
)

data class LLCallArgumentMapping(
    val argumentIndex: Int,
    val parameterName: Name?,
    val parameterType: ConeCangJieType?,
)

internal fun Candidate.toLLCall(): LLCall {
    val argumentMappings = if (argumentMappingInitialized) {
        arguments.mapIndexed { index, argumentAtom ->
            val parameter = argumentMapping[argumentAtom]
            LLCallArgumentMapping(
                argumentIndex = index,
                parameterName = parameter?.name,
                parameterType = parameter?.returnTypeRef?.coneTypeOrNull,
            )
        }
    } else {
        emptyList()
    }

    return LLCall(
        callInfo = callInfo,
        candidate = this,
        kind = callInfo.callKind,
        origin = callInfo.origin,
        applicability = lowestApplicability,
        isImplicitInvoke = callInfo.isImplicitInvoke,
        calleeName = callInfo.name,
        target = symbol as? CfirCallableSymbol<*>,
        explicitReceiverType = callInfo.explicitReceiver?.coneTypeOrNull,
        dispatchReceiverType = dispatchReceiverExpression()?.coneTypeOrNull,
        extensionReceiverType = chosenExtensionReceiverExpression()?.coneTypeOrNull,
        contextArgumentTypes = contextArguments().map(CfirExpression::coneTypeOrNull),
        argumentTypes = callInfo.arguments.map(CfirExpression::coneTypeOrNull),
        typeArguments = callInfo.typeArguments.map { typeRef -> typeRef.coneTypeOrNull },
        argumentMapping = argumentMappings,
    )
}
