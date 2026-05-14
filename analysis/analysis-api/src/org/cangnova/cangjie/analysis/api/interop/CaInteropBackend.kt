package org.cangnova.cangjie.analysis.api.interop

/**
 * 互操作后端种类。
 *
 * 表示一个外部声明面向的具体互操作目标语言/形态;
 * 用于互操作相关的诊断、渲染、IDE 提示分类。
 */
enum class CaInteropBackend {
    /** C 互操作。 */
    C,

    /** Java 互操作(声明侧)。 */
    JAVA,

    /** Java mirror — 仓颉侧为 Java 类型提供的镜像声明。 */
    JAVA_MIRROR,

    /** Java impl — 直接由 Java 后端实现的声明。 */
    JAVA_IMPL,

    /** Objective-C mirror — 仓颉侧为 Objective-C 类型提供的镜像声明。 */
    OBJC_MIRROR,

    /** Objective-C impl — 直接由 Objective-C 后端实现的声明。 */
    OBJC_IMPL,
}
