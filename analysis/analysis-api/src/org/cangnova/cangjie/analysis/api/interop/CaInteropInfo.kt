package org.cangnova.cangjie.analysis.api.interop

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner

/**
 * 声明上的互操作信息摘要。
 *
 * 把"是否为外部声明、面向哪些后端、调用约定、FFI 注解"等互操作维度的事实集中暴露,
 * 供 IDE 在签名渲染、诊断、跨语言导航中统一消费。
 */
interface CaInteropInfo : CaLifetimeOwner {
    /** 声明面向的互操作后端列表(可能同时含多个)。 */
    val backends: List<CaInteropBackend>

    /** 是否为 `foreign` 关键字标注的外部声明。 */
    val isForeignDeclaration: Boolean

    /** 是否为 `@FastNative` 之类的快速 native 调用。 */
    val isFastNative: Boolean

    /** 互操作侧的外部符号名;未指定时为 `null`。 */
    val externalName: String?

    /** 显式声明的调用约定;默认情形下为 `null`。 */
    val callingConvention: CaInteropCallingConvention?

    /** 该声明上携带的 FFI 类注解短名集合。 */
    val ffiAnnotationNames: List<String>
}
