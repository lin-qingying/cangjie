package org.cangnova.cangjie.analysis.api.impl.base.resolution

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
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
 * Analysis API 调用结果的基础实现。
 *
 * 对齐 Kotlin `analysis-api-impl-base` 的职责划分：
 * 公共调用结果对象落在 impl-base，
 * 后端仅负责把自身 snapshot 转换成这些稳定的公开模型。
 */
public class CaBaseCallInfo(
    successfulCall: CaCall?,
    calls: List<CaCall>,
    override val token: CaLifetimeToken,
) : CaCallInfo {
    private val backingSuccessfulCall: CaCall? = successfulCall
    private val backingCalls: List<CaCall> = calls

    override val successfulCall: CaCall?
        get() = withValidityAssertion { backingSuccessfulCall }

    override val calls: List<CaCall>
        get() = withValidityAssertion { backingCalls }
}

/**
 * Analysis API 单个调用视图的基础实现。
 *
 * 这个对象只承载已经规范化后的公开语义：
 * 调用种类、来源、适用性、接收者、显式类型参数与实参映射。
 * 它不回放底层 CFIR candidate，也不缓存任何字符串化结果。
 */
public class CaBaseCall(
    kind: CaCallKind,
    origin: CaCallOrigin,
    applicability: CaCallApplicability,
    isImplicitInvoke: Boolean,
    calleeName: Name?,
    target: CaCallableSymbol?,
    explicitReceiverType: CaType?,
    dispatchReceiverType: CaType?,
    extensionReceiverType: CaType?,
    contextArgumentTypes: List<CaType?>,
    argumentTypes: List<CaType?>,
    typeArguments: List<CaType?>,
    argumentMapping: List<CaCallArgumentMapping>,
    override val token: CaLifetimeToken,
) : CaCall {
    private val backingKind: CaCallKind = kind
    private val backingOrigin: CaCallOrigin = origin
    private val backingApplicability: CaCallApplicability = applicability
    private val backingIsImplicitInvoke: Boolean = isImplicitInvoke
    private val backingCalleeName: Name? = calleeName
    private val backingTarget: CaCallableSymbol? = target
    private val backingExplicitReceiverType: CaType? = explicitReceiverType
    private val backingDispatchReceiverType: CaType? = dispatchReceiverType
    private val backingExtensionReceiverType: CaType? = extensionReceiverType
    private val backingContextArgumentTypes: List<CaType?> = contextArgumentTypes
    private val backingArgumentTypes: List<CaType?> = argumentTypes
    private val backingTypeArguments: List<CaType?> = typeArguments
    private val backingArgumentMapping: List<CaCallArgumentMapping> = argumentMapping

    override val kind: CaCallKind
        get() = withValidityAssertion { backingKind }

    override val origin: CaCallOrigin
        get() = withValidityAssertion { backingOrigin }

    override val applicability: CaCallApplicability
        get() = withValidityAssertion { backingApplicability }

    override val isImplicitInvoke: Boolean
        get() = withValidityAssertion { backingIsImplicitInvoke }

    override val calleeName: Name?
        get() = withValidityAssertion { backingCalleeName }

    override val target: CaCallableSymbol?
        get() = withValidityAssertion { backingTarget }

    override val explicitReceiverType: CaType?
        get() = withValidityAssertion { backingExplicitReceiverType }

    override val dispatchReceiverType: CaType?
        get() = withValidityAssertion { backingDispatchReceiverType }

    override val extensionReceiverType: CaType?
        get() = withValidityAssertion { backingExtensionReceiverType }

    override val contextArgumentTypes: List<CaType?>
        get() = withValidityAssertion { backingContextArgumentTypes }

    override val argumentTypes: List<CaType?>
        get() = withValidityAssertion { backingArgumentTypes }

    override val typeArguments: List<CaType?>
        get() = withValidityAssertion { backingTypeArguments }

    override val argumentMapping: List<CaCallArgumentMapping>
        get() = withValidityAssertion { backingArgumentMapping }
}

/**
 * Analysis API 实参与形参映射的基础实现。
 *
 * 映射对象只暴露公开层需要消费的结构化信息，
 * 不保留底层调用图节点或文本回放结果。
 */
public class CaBaseCallArgumentMapping(
    argumentIndex: Int,
    parameterName: Name?,
    parameterType: CaType?,
    override val token: CaLifetimeToken,
) : CaCallArgumentMapping {
    private val backingArgumentIndex: Int = argumentIndex
    private val backingParameterName: Name? = parameterName
    private val backingParameterType: CaType? = parameterType

    override val argumentIndex: Int
        get() = withValidityAssertion { backingArgumentIndex }

    override val parameterName: Name?
        get() = withValidityAssertion { backingParameterName }

    override val parameterType: CaType?
        get() = withValidityAssertion { backingParameterType }
}
