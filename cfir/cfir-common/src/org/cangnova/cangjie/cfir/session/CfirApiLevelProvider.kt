package org.cangnova.cangjie.cfir.session

/**
 * 项目级 APILevel 提供方。
 *
 * 对齐 C++ `globalOptions.apiLevel` 选项:编译驱动从命令行或 ohos 目标配置读入,
 * 标识当前模块允许引用的最高 APILevel。超过该级别的声明引用报告
 * `APILEVEL_REF_HIGHER`,相关 syscap 检查用 `APILEVEL_SYSCAP_WARNING/_ERROR`。
 *
 * - [projectApiLevel] 默认 [DISABLED],表示未启用 APILevel 检查(非 ohos 目标)。
 * - [syscapUnion] 所有目标设备可能支持的 syscap 并集;目标声明 @Syscap(s),
 *   s ∉ union ⇒ `APILEVEL_SYSCAP_ERROR`。
 * - [syscapIntersection] 所有目标设备都支持的 syscap 交集;s ∉ intersection ⇒
 *   `APILEVEL_SYSCAP_WARNING`。
 * - [syscapEnabled] syscap 检查总开关,false 时跳过 union/intersection 比对。
 */
interface CfirApiLevelProvider : CfirSessionComponent {
    /**
     * 当前模块允许引用的最高 APILevel。
     */
    val projectApiLevel: Int

    /**
     * 是否启用 syscap 并集/交集检查。
     */
    val syscapEnabled: Boolean get() = false

    /**
     * 所有目标设备可能支持的 syscap 并集。
     */
    val syscapUnion: Set<String> get() = emptySet()

    /**
     * 所有目标设备共同支持的 syscap 交集。
     */
    val syscapIntersection: Set<String> get() = emptySet()

    /**
     * APILevel 提供方使用的公共常量。
     */
    companion object {
        /**
         * 表示未启用 APILevel 检查。
         */
        const val DISABLED: Int = -1
    }
}

/** 默认实现:APILevel 检查关闭。 */
object DefaultCfirApiLevelProvider : CfirApiLevelProvider {
    /**
     * 默认禁用 APILevel 检查。
     */
    override val projectApiLevel: Int get() = CfirApiLevelProvider.DISABLED
}

/**
 * 从 session 中读取项目级 APILevel 提供方。
 *
 * 未显式注册时返回 [DefaultCfirApiLevelProvider]。
 */
val CfirSession.apiLevelProvider: CfirApiLevelProvider by CfirSession.sessionComponentAccessorWithDefault(
    DefaultCfirApiLevelProvider
)
