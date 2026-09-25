package org.cangnova.cangjie.cfir.session

/**
 * 当前编译调用的 CJMP（common/specific 跨平台声明族）编译模式。
 *
 * 判据对齐官方 `GlobalOptions` 的模式谓词：
 * - `IsCompilingCJMPSpecific() = commonPartChirs 非空` → [SPECIFIC]；
 * - `IsCompilingCJMP() = Specific || outputMode == CHIR` → [COMMON]（CHIR 输出模式即编译 common part）；
 * - 两个谓词皆假 → [NONE]（官方 `CheckCJMPModifiers` 在此报 `parse_unexpected_cjmp_decl`）。
 *
 * 模式是**编译调用**的事实，不是语言版本的事实：1.1+ 的语言表面在 mode=None 下依然不合法，
 * 只是诊断换成 `parse_unexpected_cjmp_decl`（计划 §8.1 三层门的第 ② 层）。
 */
public enum class CfirCjmpMode {
    /** 未开启 CJMP 编译（既非 common part 也非 specific part）。 */
    NONE,

    /** 编译 common part（CHIR 输出模式）。 */
    COMMON,

    /** 编译 specific part（已加载 common part 的 cjo/chir）。 */
    SPECIFIC,
}

/**
 * 源码 session 的 CJMP 编译模式与 common-part 输入。
 *
 * 与 [CfirInteropSettingsComponent] 同构：模式事实由 driver 选项进入 session，
 * checker 不得从源码形状反推。**per-session 语义**（D14）：多模块编译中每个模块 session
 * 持有自己的组件值——源模块由 driver options 决定，测试模块由 `// CJMP_MODE` 指令决定，
 * IDE 由模块 kind 决定（未落地前为 [CfirCjmpMode.NONE]）。
 *
 * @property commonPartCjoPaths common part `.cjo` 输入路径（官方 `--common-part-cjo`）。
 * @property commonPartChirPaths common part `.chir` 输入路径（官方 `--common-part-chir`）；
 *   非空即 specific 编译（官方 `IsCompilingCJMPSpecific()` 判据）。
 * @property isChirOutput 当前调用是否为 CHIR 输出模式（官方 `IsCompilingCJMP()` 的 Common 判据）。
 * @property explicitMode 显式模式覆盖（测试指令 / IDE 模块 kind）；为 null 时由前三项推导。
 * @property packageFeatures 当前编译包的 features 集合（官方 `CollectFeaturesFromPackage`：
 *   来自源文件的 `features { ... }` 指令）；features 子集门用它比对 common part。
 * @property moduleDebug 写出/加载 common part cjo 时的 debug 选项（官方 `Option::debug`）。
 * @property moduleOptLevel 优化级别字符串（官方 `Option` 的优化级别，`O0`/`O1`/…）。
 */
public open class CfirCjmpSettingsComponent(
    /** 官方 `--common-part-cjo` 输入路径列表。 */
    val commonPartCjoPaths: List<String> = emptyList(),
    /** 官方 `--common-part-chir` 输入路径列表。 */
    val commonPartChirPaths: List<String> = emptyList(),
    /** 当前调用是否为 CHIR 输出模式。 */
    val isChirOutput: Boolean = false,
    /** 显式模式覆盖；null 表示按官方谓词推导。 */
    val explicitMode: CfirCjmpMode? = null,
    /** 当前编译包的 features 集合（官方 `File::GetFeatures()` 的包级并集）。 */
    val packageFeatures: Set<String> = emptySet(),
    /** 写出/加载 common part cjo 时的 debug 选项（官方 `Option::debug`）。 */
    val moduleDebug: Boolean = false,
    /** 优化级别字符串（`O0`..`Oz`），对齐官方 `module_common_cjo_opt_mismatch` 诊断文案。 */
    val moduleOptLevel: String = DEFAULT_OPT_LEVEL,
) : CfirSessionComponent {
    /** 当前 session 的 CJMP 编译模式。 */
    public val mode: CfirCjmpMode
        get() = explicitMode ?: when {
            commonPartChirPaths.isNotEmpty() -> CfirCjmpMode.SPECIFIC
            isChirOutput -> CfirCjmpMode.COMMON
            else -> CfirCjmpMode.NONE
        }

    companion object {
        /** 官方 `OptimizationLevel::O0` 的字符串形；未显式配置时的默认优化级别。 */
        public const val DEFAULT_OPT_LEVEL: String = "O0"

        /**
         * 两个 common-part 选项的数量配对校验（官方
         * `driver_require_common_chir_for_each_common_cjo` 对位）。
         *
         * 官方在 driver 层拒绝"有 common cjo 却没有对应 chir"的调用；本仓库没有独立
         * driver 进程，校验落在配置 → session 组件的唯一转换点上。
         */
        public fun validateCommonPartInputs(
            commonPartCjoPaths: List<String>,
            commonPartChirPaths: List<String>,
        ) {
            require(commonPartCjoPaths.size == commonPartChirPaths.size) {
                "require common chir for each common cjo: ${commonPartCjoPaths.size} common-part " +
                    "cjo path(s) but ${commonPartChirPaths.size} common-part chir path(s)"
            }
        }
    }
}

/** 未配置 CJMP 编译时使用的不可变默认组件（mode = [CfirCjmpMode.NONE]）。 */
public object DefaultCfirCjmpSettingsComponent : CfirCjmpSettingsComponent()

/** 当前 session 的 CJMP 编译模式配置。 */
public val CfirSession.cjmpSettings: CfirCjmpSettingsComponent by
    CfirSession.sessionComponentAccessorWithDefault(DefaultCfirCjmpSettingsComponent)
