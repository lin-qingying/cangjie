package org.cangnova.cangjie.cfir.session

/**
 * 当前源码编译目标使用的互操作语言。
 *
 * 该值对应官方编译器的 `GlobalOptions::InteropLanguage`。它只表示目标配置，
 * 不表示某个声明已经被派生为 CJMapping；声明级派生状态由 CFIR interop producer
 * 根据该配置、声明可见性和声明种类计算。
 */
public enum class CfirInteropTarget {
    /** 未选择 Java 或 Objective-C 互操作目标。 */
    NONE,

    /** Java 互操作目标。 */
    JAVA,

    /** Objective-C 互操作目标。 */
    OBJC,
}

/**
 * 源码 session 的互操作配置。
 *
 * [enableInteropCJMapping] 与 [targetInteropLanguage] 必须同时进入 session，
 * 不能由 checker 从源码注解名称反推。这样 source、macro 和后续 binary consumer
 * 使用同一份配置事实，且未启用时不会伪造 CJMapping metadata。
 */
public open class CfirInteropSettingsComponent(
    /** 是否启用官方 `--enable-interop-cjmapping`。 */
    val enableInteropCJMapping: Boolean = false,
    /** 当前 CJMapping 目标语言。 */
    val targetInteropLanguage: CfirInteropTarget = CfirInteropTarget.NONE,
    /** 可选的 CJMapping package instance 配置路径。 */
    val interopCJPackageConfigPath: String? = null,
) : CfirSessionComponent

/** 未配置互操作时使用的不可变默认组件。 */
public object DefaultCfirInteropSettingsComponent : CfirInteropSettingsComponent()

/** 当前 session 的互操作配置。 */
public val CfirSession.interopSettings: CfirInteropSettingsComponent by
    CfirSession.sessionComponentAccessorWithDefault(DefaultCfirInteropSettingsComponent)
