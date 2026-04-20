package org.cangnova.cangjie.analysis.api.resolution

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.name.Name

/**
 * 单个调用的公开语义视图。
 *
 * 该视图稳定暴露调用目标、适用性、接收者拆分、显式类型参数以及实参与形参映射，
 * 供 Analysis API 测试框架、引用服务、渲染器和上层工具共享。
 */
interface CaCall : CaLifetimeOwner {
    val kind: CaCallKind

    val origin: CaCallOrigin

    val applicability: CaCallApplicability

    val isImplicitInvoke: Boolean

    val calleeName: Name?

    val target: CaCallableSymbol?

    val explicitReceiverType: CaType?

    val dispatchReceiverType: CaType?

    val extensionReceiverType: CaType?

    val contextArgumentTypes: List<CaType?>

    val argumentTypes: List<CaType?>

    val typeArguments: List<CaType?>

    val argumentMapping: List<CaCallArgumentMapping>

    val typeArgumentCount: Int
        get() = typeArguments.size
}

val CaCall.isSuccessful: Boolean
    get() = applicability.isSuccess
