package org.cangnova.cangjie.cfir.session

import org.cangnova.cangjie.config.CjmpCommonPartInputs
import java.nio.file.Path
import java.util.Collections

/**
 * 当前编译调用的 CJMP（common/specific 跨平台声明族）编译模式。
 *
 * 模式由统一输入 owner 推导：
 * - 有 common CJO parent，或旧版 driver 收到位置 CHIR 输入 → [SPECIFIC]；
 * - CHIR 输出模式 → [COMMON]；
 * - 两个谓词皆假 → [NONE]；官方 parser 根据实际修饰符报告文件角色错位。
 *
 * CLI/configuration 已按语言版本协议归一化输入；session 只消费 parent 列表，不重复解释选项协议。
 * 模式是**编译调用**的事实，不是语言版本的事实：1.1+ 的语言表面在 mode=None 下依然不合法，
 * 普通编译中的 common/specific 修饰符分别违反 common/specific 文件角色要求。
 */
public enum class CfirCjmpMode {
    /** 未开启 CJMP 编译（既非 common part 也非 specific part）。 */
    NONE,

    /** 编译 common part（CHIR 输出模式）。 */
    COMMON,

    /** 编译 specific part（已加载 common part CJO）。 */
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
 * @property commonPartInputs 经过语言版本协议归一化的 common-part parent 输入。
 * @property positionalChirInputs 旧版 driver 的 raw CHIR 位置输入（官方 `inputChirFiles`）；该输入本身也启用 specific 判据。
 * @property isChirOutput 当前调用是否为 CHIR 输出模式（官方 `IsCompilingCJMP()` 的 Common 判据）。
 * @property explicitMode 显式模式覆盖（测试指令 / IDE 模块 kind）；为 null 时由输入事实推导。
 * @property packageFeaturesByPackage 每个 specific 源包的 features 并集（官方
 *   `CollectFeaturesFromPackage`）；features 子集门按请求包名选取，避免同一编译模块内其他包的
 *   feature 泄漏到该加载门。
 * @property moduleDebug 写出/加载 common part cjo 时的 debug 选项（官方 `Option::debug`）。
 * @property moduleOptLevel 写出/加载 common part cjo 时的优化级别。
 */
public open class CfirCjmpSettingsComponent(
    /** 由 CLI/configuration 的统一输入 owner 生成，不在 session 层重新推断版本语义。 */
    public val commonPartInputs: CjmpCommonPartInputs = CjmpCommonPartInputs.EMPTY,
    /** 旧版 positional `inputChirFiles` 输入快照；不得与 alpha.20 parent 配对 CHIR 混淆。 */
    positionalChirInputs: List<Path> = emptyList(),
    /** 当前调用是否为 CHIR 输出模式。 */
    val isChirOutput: Boolean = false,
    /** 显式模式覆盖；null 表示按官方谓词推导。 */
    val explicitMode: CfirCjmpMode? = null,
    /** 每个源码包的 features 集合（官方 `File::GetFeatures()` 的包级并集）。 */
    val packageFeaturesByPackage: Map<String, Set<String>> = emptyMap(),
    /** 写出/加载 common part cjo 时的 debug 选项（官方 `Option::debug`）。 */
    val moduleDebug: Boolean = false,
    /** 写出/加载 common part cjo 时的优化级别。 */
    val moduleOptLevel: CfirCjmpOptimizationLevel = CfirCjmpOptimizationLevel.O0,
) : CfirSessionComponent {
    public val positionalChirInputs: List<Path> =
        Collections.unmodifiableList(ArrayList(positionalChirInputs))

    /**
     * 用 raw CFIR 收集到的包级 features 替换配置阶段的值。
     * PSI 与 LightTree 在此之前已归一化为同一套文件模型。
     */
    public fun withPackageFeatures(
        packageFeaturesByPackage: Map<String, Set<String>>,
    ): CfirCjmpSettingsComponent =
        CfirCjmpSettingsComponent(
            commonPartInputs = commonPartInputs,
            positionalChirInputs = positionalChirInputs,
            isChirOutput = isChirOutput,
            explicitMode = explicitMode,
            packageFeaturesByPackage = packageFeaturesByPackage.mapValues { (_, features) -> features.toSet() },
            moduleDebug = moduleDebug,
            moduleOptLevel = moduleOptLevel,
        )

    /** 归一化 parent 的 `.cjo` 路径，供 explicit preload 与 depends-on 模块图共同消费。 */
    public val commonPartCjoPaths: List<String> get() = commonPartInputs.commonPartCjoPaths

    /** 归一化 parent 的 `.chir` 路径，供 alpha.20 起的输入配对与 CHIR 链接消费。 */
    public val commonPartChirPaths: List<String> get() = commonPartInputs.commonPartChirPaths

    /** 官方 MPParserImpl::SetCompileOptions 的 common 谓词，与 specific 谓词独立。 */
    public val compileCommon: Boolean
        get() = explicitMode?.let { it == CfirCjmpMode.COMMON } ?: isChirOutput

    /** 输入 common-part CJO 或旧版 positional CHIR 时启用 specific；不从源码形状反推。 */
    public val compileSpecific: Boolean
        get() = explicitMode?.let { it == CfirCjmpMode.SPECIFIC }
            ?: (commonPartCjoPaths.isNotEmpty() || positionalChirInputs.isNotEmpty())

    /** 当前 session 的 CJMP 编译模式。 */
    public val mode: CfirCjmpMode
        get() = explicitMode ?: when {
            commonPartInputs.isSpecificCompilation || positionalChirInputs.isNotEmpty() -> CfirCjmpMode.SPECIFIC
            isChirOutput -> CfirCjmpMode.COMMON
            else -> CfirCjmpMode.NONE
        }
}

/** 未配置 CJMP 编译时使用的不可变默认组件（mode = [CfirCjmpMode.NONE]）。 */
public object DefaultCfirCjmpSettingsComponent : CfirCjmpSettingsComponent()

/** 当前 session 的 CJMP 编译模式配置。 */
public val CfirSession.cjmpSettings: CfirCjmpSettingsComponent by
    CfirSession.sessionComponentAccessorWithDefault(DefaultCfirCjmpSettingsComponent)
