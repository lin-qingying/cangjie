package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.resolve.CaCfirCallApplicability
import org.cangnova.cangjie.analysis.api.cfir.resolve.CaCfirCallArgumentMappingSnapshot
import org.cangnova.cangjie.analysis.api.cfir.resolve.CaCfirCallInfoSnapshot
import org.cangnova.cangjie.analysis.api.cfir.resolve.CaCfirCallKind
import org.cangnova.cangjie.analysis.api.cfir.resolve.CaCfirCallOrigin
import org.cangnova.cangjie.analysis.api.cfir.resolve.CaCfirCallSnapshot
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.resolution.CaCall
import org.cangnova.cangjie.analysis.api.resolution.CaCallApplicability
import org.cangnova.cangjie.analysis.api.resolution.CaCallArgumentMapping
import org.cangnova.cangjie.analysis.api.resolution.CaCallInfo
import org.cangnova.cangjie.analysis.api.resolution.CaCallKind
import org.cangnova.cangjie.analysis.api.resolution.CaCallOrigin
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.name.Name

/**
 * CFIR 调用结果到公开 Analysis API 调用结果的桥接实现。
 *
 * 这一层只负责把 low-level 调用快照稳定映射成 `CaCallInfo` / `CaCall`，
 * 不在这里重做调用解析，也不重新引入 CFIR 候选对象。
 */
internal class CaCfirCallInfoImpl(
    override val successfulCall: CaCall?,
    override val calls: List<CaCall>,
    override val token: CaLifetimeToken,
) : CaCallInfo

internal class CaCfirCallImpl(
    override val kind: CaCallKind,
    override val origin: CaCallOrigin,
    override val applicability: CaCallApplicability,
    override val isImplicitInvoke: Boolean,
    override val calleeName: Name?,
    override val target: CaCallableSymbol?,
    override val explicitReceiverType: CaType?,
    override val dispatchReceiverType: CaType?,
    override val extensionReceiverType: CaType?,
    override val contextArgumentTypes: List<CaType?>,
    override val argumentTypes: List<CaType?>,
    override val typeArguments: List<CaType?>,
    override val argumentMapping: List<CaCallArgumentMapping>,
    override val token: CaLifetimeToken,
) : CaCall

internal class CaCfirCallArgumentMappingImpl(
    override val argumentIndex: Int,
    override val parameterName: Name?,
    override val parameterType: CaType?,
    override val token: CaLifetimeToken,
) : CaCallArgumentMapping

internal fun CaCfirCallInfoSnapshot.asCaCallInfo(
    analysisSession: CaCfirSession,
    token: CaLifetimeToken,
): CaCallInfo {
    val mappedCalls = calls.map { callSnapshot -> callSnapshot.asCaCall(analysisSession, token) }
    val mappedSuccessfulCall = successfulCall?.asCaCall(analysisSession, token)
    return CaCfirCallInfoImpl(
        successfulCall = mappedSuccessfulCall,
        calls = mappedCalls,
        token = token,
    )
}

private fun CaCfirCallSnapshot.asCaCall(
    analysisSession: CaCfirSession,
    token: CaLifetimeToken,
): CaCall {
    return CaCfirCallImpl(
        kind = kind.asAnalysisKind(),
        origin = origin.asAnalysisOrigin(),
        applicability = applicability.asAnalysisApplicability(),
        isImplicitInvoke = isImplicitInvoke,
        calleeName = calleeName,
        target = target?.let(analysisSession::getPublicSymbol) as? CaCallableSymbol,
        explicitReceiverType = explicitReceiverType?.asCaType(token),
        dispatchReceiverType = dispatchReceiverType?.asCaType(token),
        extensionReceiverType = extensionReceiverType?.asCaType(token),
        contextArgumentTypes = contextArgumentTypes.map { argumentType -> argumentType?.asCaType(token) },
        argumentTypes = argumentTypes.map { argumentType -> argumentType?.asCaType(token) },
        typeArguments = typeArguments.map { typeArgument -> typeArgument?.asCaType(token) },
        argumentMapping = argumentMapping.map { mapping ->
            mapping.asCaCallArgumentMapping(token)
        },
        token = token,
    )
}

private fun CaCfirCallKind.asAnalysisKind(): CaCallKind = when (this) {
    CaCfirCallKind.FUNCTION -> CaCallKind.FUNCTION
}

private fun CaCfirCallOrigin.asAnalysisOrigin(): CaCallOrigin = when (this) {
    CaCfirCallOrigin.REGULAR -> CaCallOrigin.REGULAR
    CaCfirCallOrigin.OPERATOR -> CaCallOrigin.OPERATOR
}

private fun CaCfirCallApplicability.asAnalysisApplicability(): CaCallApplicability = when (this) {
    CaCfirCallApplicability.HIDDEN -> CaCallApplicability.HIDDEN
    CaCfirCallApplicability.INAPPLICABLE_WRONG_RECEIVER -> CaCallApplicability.INAPPLICABLE_WRONG_RECEIVER
    CaCfirCallApplicability.INAPPLICABLE_ARGUMENTS_MAPPING_ERROR ->
        CaCallApplicability.INAPPLICABLE_ARGUMENTS_MAPPING_ERROR
    CaCfirCallApplicability.INAPPLICABLE -> CaCallApplicability.INAPPLICABLE
    CaCfirCallApplicability.VISIBILITY_ERROR -> CaCallApplicability.VISIBILITY_ERROR
    CaCfirCallApplicability.UNSAFE_CALL -> CaCallApplicability.UNSAFE_CALL
    CaCfirCallApplicability.UNSTABLE_SMARTCAST -> CaCallApplicability.UNSTABLE_SMARTCAST
    CaCfirCallApplicability.CONVENTION_ERROR -> CaCallApplicability.CONVENTION_ERROR
    CaCfirCallApplicability.RESOLVED_LOW_PRIORITY -> CaCallApplicability.RESOLVED_LOW_PRIORITY
    CaCfirCallApplicability.RESOLVED_NEED_PRESERVE_COMPATIBILITY ->
        CaCallApplicability.RESOLVED_NEED_PRESERVE_COMPATIBILITY
    CaCfirCallApplicability.RESOLVED_WITH_ERROR -> CaCallApplicability.RESOLVED_WITH_ERROR
    CaCfirCallApplicability.RESOLVED -> CaCallApplicability.RESOLVED
}

private fun CaCfirCallArgumentMappingSnapshot.asCaCallArgumentMapping(
    token: CaLifetimeToken,
): CaCallArgumentMapping {
    return CaCfirCallArgumentMappingImpl(
        argumentIndex = argumentIndex,
        parameterName = parameterName,
        parameterType = parameterType?.asCaType(token),
        token = token,
    )
}
