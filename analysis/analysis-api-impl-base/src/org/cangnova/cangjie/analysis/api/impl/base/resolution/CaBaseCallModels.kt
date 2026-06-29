package org.cangnova.cangjie.analysis.api.impl.base.resolution

import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.diagnostics.CaDiagnostic
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.resolution.CaCall
import org.cangnova.cangjie.analysis.api.resolution.CaCallApplicability
import org.cangnova.cangjie.analysis.api.resolution.CaCallInfo
import org.cangnova.cangjie.analysis.api.resolution.CaCallKind
import org.cangnova.cangjie.analysis.api.resolution.CaCallOrigin
import org.cangnova.cangjie.analysis.api.resolution.CaErrorCallInfo
import org.cangnova.cangjie.analysis.api.resolution.CaSuccessCallInfo
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.name.Name

/**
 * 成功解析调用信息的基础实现。
 */
@CaImplementationDetail
class CaBaseSuccessCallInfo(
    /**
     * 成功解析出的调用模型。
     */
    private val backingCall: CaCall,
) : CaSuccessCallInfo {
    /**
     * 成功调用信息沿用调用模型的 lifetime token。
     */
    override val token: CaLifetimeToken get() = backingCall.token

    /**
     * 返回成功解析出的调用模型。
     */
    override val call: CaCall get() = withValidityAssertion { backingCall }
}


/**
 * 解析失败调用信息的基础实现。
 */
@CaImplementationDetail
class CaBaseErrorCallInfo(
    candidateCalls: List<CaCall>,
    /**
     * 描述调用失败原因的诊断。
     */
    private val backingDiagnostic: CaDiagnostic,
) : CaErrorCallInfo {
    /**
     * 失败路径中仍可恢复的候选调用列表。
     */
    private val backingCandidateCalls: List<CaCall> = candidateCalls

    /**
     * 错误调用信息沿用诊断的 lifetime token。
     */
    override val token: CaLifetimeToken get() = backingDiagnostic.token

    /**
     * 返回候选调用列表。
     */
    override val candidateCalls: List<CaCall> get() = withValidityAssertion { backingCandidateCalls }

    /**
     * 返回调用失败诊断。
     */
    override val diagnostic: CaDiagnostic get() = withValidityAssertion { backingDiagnostic }
}
