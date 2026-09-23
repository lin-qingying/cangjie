package org.cangnova.cangjie

import org.cangnova.cangjie.util.DescriptionAware
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import java.util.*

/**
 * 仓颉语言版本枚举。
 *
 * 按官方三段版本号组织，承载语言语义演进的兼容判定：稳定性以 [LATEST_STABLE]
 * 为界，弃用与支持范围由 companion 中的边界常量定义。
 *
 * @property major 主版本号，表达不兼容语言语义变化。
 * @property minor 次版本号，表达向后兼容的语言能力扩展。
 * @property patch 修订版本号，表达补丁级语言行为或标准库同步版本。
 * @property preReleaseTag 预发布标签；非空表示该版本为预发布版本。
 */
enum class LanguageVersion(
    /**
     * 主版本号，表达不兼容语言语义变化。
     */
    val major: Int,
    /**
     * 次版本号，表达向后兼容的语言能力扩展。
     */
    val minor: Int,
    /**
     * 修订版本号，表达补丁级语言行为或标准库同步版本。
     */
    val patch: Int,
    val preReleaseTag: String? = null
) : DescriptionAware {

    CANGJIE_1_0_0(1, 0, 0),
    CANGJIE_1_0_1(1, 0, 1),
    CANGJIE_1_0_2(1, 0, 2),
    CANGJIE_1_0_3(1, 0, 3),
    CANGJIE_1_0_4(1, 0, 4),
    CANGJIE_1_0_5(1, 0, 5),
    CANGJIE_1_1_0(1, 1, 0),
    CANGJIE_1_1_3(1, 1, 3);


    val isStable: Boolean
        get() = this <= LATEST_STABLE


    val isDeprecated: Boolean
        get() = this in FIRST_SUPPORTED..<FIRST_NON_DEPRECATED

    val isUnsupported: Boolean
        get() = this < FIRST_SUPPORTED

    /**
     * 保留完整的三段版本号。补丁号参与版本排序和 feature 门禁，不能在
     * 展示或配置往返时折叠掉，否则 `1.0.5` 会退化为 `1.0.0`。
     */
    val versionString: String = "$major.$minor.$patch"

    override val description: String
        get() = when {
            !isStable -> "$versionString (experimental)"
            isDeprecated -> "$versionString (deprecated)"
            isUnsupported -> "$versionString (unsupported)"
            else -> versionString
        }

    override fun toString() = versionString

    companion object {
        fun parse(versionString: String): LanguageVersion {
            val parts = versionString.split('.')
            if (parts.size !in 2..3) error("Invalid version string: $versionString")
            val major = parts[0].toIntOrNull() ?: error("Invalid major version: ${parts[0]}")
            val minor = parts[1].toIntOrNull() ?: error("Invalid minor version: ${parts[1]}")
            val patch = if (parts.size > 2) parts[2].toIntOrNull() ?: error("Invalid patch version: ${parts[2]}") else 0
            return entries.firstOrNull { it.major == major && it.minor == minor && it.patch == patch }
                ?: error("Version not found: $versionString")
        }

        @JvmField
        val FIRST_SUPPORTED = CANGJIE_1_0_0

        @JvmField
        val FIRST_NON_DEPRECATED = CANGJIE_1_0_0

        @JvmField
        /** 当前支持矩阵中的最新稳定语言版本。 */
        val LATEST_STABLE = CANGJIE_1_1_3

    }
}

/**
 * 语言特性在越过 sinceVersion 之后的行为策略。
 *
 * 默认 [CannotBeDisabled] 表示特性到达目标版本后强制启用、无法关闭；
 * [CanStillBeDisabledForNow] 表示短期内仍允许显式关闭，附带关联的工单号。
 */
sealed class LanguageFeatureBehaviorAfterSinceVersion {
    /** 特性已定型，越过 since 版本后不可再关闭。 */
    data object CannotBeDisabled : LanguageFeatureBehaviorAfterSinceVersion()

    /** 特性越过 since 版本后短期内仍可关闭，[relevantTicketId] 记录最终收口工单。 */
    data class CanStillBeDisabledForNow(val relevantTicketId: String) : LanguageFeatureBehaviorAfterSinceVersion()
}

/** 未指定关联工单时的占位文本。 */
const val NO_ISSUE_SPECIFIED = "No issue"

/**
 * 可由语言版本设置显式开启的语言特性。
 *
 * 这些开关用于在同一编译器中承载实验特性、兼容行为和诊断策略差异。
 */
enum class LanguageFeature(
    val sinceVersion: LanguageVersion?,
    val issue: String = NO_ISSUE_SPECIFIED,
    private val enabledInProgressiveMode: Boolean = false,
    val forcesPreReleaseBinaries: Boolean = false,
    val testOnly: Boolean = false,
    val hintUrl: String? = null,
    val behaviorAfterSinceVersion: LanguageFeatureBehaviorAfterSinceVersion = LanguageFeatureBehaviorAfterSinceVersion.CannotBeDisabled,
) {
    /**
     * Enables data-flow-analysis based warnings in frontend diagnostics.
     */
    EnableDfaWarnings(LanguageVersion.CANGJIE_1_0_0),

    /**
     * Reports lambda/function value mismatch as ARGUMENT_TYPE_MISMATCH instead of
     * RETURN_TYPE_MISMATCH on lambda body return expression.
     */
    LambdaReturnTypeMismatchAsArgumentTypeMismatch(LanguageVersion.CANGJIE_1_0_0),
    InvalidBinaryOperatorDiagnostics(LanguageVersion.CANGJIE_1_0_0),
    LexicographicVariableReadinessCalculation(LanguageVersion.CANGJIE_1_0_0),

    /**
     * Effect handlers（`perform`/`resume`/`handle`/`throwing` 语法与 `Command<T>` 语义）。
     *
     * 实验特性，默认全版本关闭（sinceVersion = null）：官方将 effect handlers 定位为
     * 实验性 opt-in 特性（官方测试体系以 `%enableEH` 显式开启；截至 OCX 2026 公开介绍时
     * 官方仍标注"积极开发中的实验性部分"，没有任何已发布官方编译器版本包含它，
     * 已开源镜像亦无实现）。关闭时 perform/resume/handle 在 resolve 层报
     * EFFECTS_FEATURE_DISABLED；语法层因 perform/resume/throwing 为硬关键字仍可解析。
     *
     * 毕业条件（满足后才钉 sinceVersion，对齐 JavaInteropAnnotations 形态）：
     * ① 官方规范/发布说明将 effect handlers 列入稳定语言表面；② 本仓库 stdlib
     * 提供 stdx.effect 包；③ perform→handle→resume 端到端 LLT 通过。
     * 毕业时钉 sinceVersion，并以 CanStillBeDisabledForNow 设过渡窗。
     */
    EffectHandlers(null),

    /** v1.0.0 parser/AST language builtin annotation surface. */
    BuiltInAnnotations(LanguageVersion.CANGJIE_1_0_0),

    /** v1.0.0 system availability macros and the IfAvailable expression family. */
    AvailabilityAnnotations(LanguageVersion.CANGJIE_1_0_0),

    /** v1.0.0 declaration-source (`.cj.d`) compilation mode. */
    DeclarationFiles(LanguageVersion.CANGJIE_1_0_0),

    /** OpenHarmony APILevel `since: String` parameter introduced after the v1.0.0 schema. */
    ApiLevelSinceParameter(LanguageVersion.CANGJIE_1_0_5),

    /**
     * `sema_unused_import` 检查（v1.0.2 起）。官方分界以版本矩阵为准：v1.0.0 双 SDK 实测
     * 零诊断，v1.0.5 起报 `unused import`；开源快照（external/cangjie_compiler）无该诊断，
     * 无法从源码进一步收窄边界，故采信官方 v1.0.2 首现记录。
     */
    UnusedImportCheck(LanguageVersion.CANGJIE_1_0_2),

    /** Official AST/metadata-only `@Java` identity, present in the 1.0.0 baseline. */
    JavaBuiltinAnnotations(LanguageVersion.CANGJIE_1_0_0),

    /**
     * 本项目对官方 v1.1.0 parser/AST 互操作身份启用的兼容门禁。
     *
     * 官方 cjc 本身没有 Kotlin 式 `-language-version` feature 配置；
     * 这里是 CFIR 为 1.0.0 起点提供的版本化分析策略。官方 parser
     * schema 的引入证据由 annotation catalog 保存，不能把该枚举误读为 cjc
     * 原生诊断名称。
     */
    JavaInteropAnnotations(
        LanguageVersion.CANGJIE_1_1_0,
        behaviorAfterSinceVersion = LanguageFeatureBehaviorAfterSinceVersion.CanStillBeDisabledForNow(NO_ISSUE_SPECIFIED),
    ),

    /** Objective-C mirror/implementation annotation family. */
    ObjCInteropAnnotations(
        LanguageVersion.CANGJIE_1_1_0,
        behaviorAfterSinceVersion = LanguageFeatureBehaviorAfterSinceVersion.CanStillBeDisabledForNow(NO_ISSUE_SPECIFIED),
    ),

    /** ForeignName/ForeignGetterName/ForeignSetterName metadata. */
    InteropForeignNameAnnotations(
        LanguageVersion.CANGJIE_1_1_0,
        behaviorAfterSinceVersion = LanguageFeatureBehaviorAfterSinceVersion.CanStillBeDisabledForNow(NO_ISSUE_SPECIFIED),
    ),

    /** `features { @NonProduct ... }` package/product metadata introduced in 1.1.0. */
    PackageProductMetadata(
        LanguageVersion.CANGJIE_1_1_0,
        behaviorAfterSinceVersion = LanguageFeatureBehaviorAfterSinceVersion.CanStillBeDisabledForNow(NO_ISSUE_SPECIFIED),
    ),

    /**
     * 同包内多个同名顶层 private nominal 声明均需导出时报 `EXPORT_SAME_PRIVATE_DECL`。
     *
     * 官方于 v1.0.2 引入（提交 `e3200e1`，2025-09-16，`CheckFunctionLinkage.cpp`
     * `AnalyzeFunctionLinkage` 末尾遍历 `IsNominalDecl() && private && linkage != INTERNAL`
     * 的声明）；v1.0.0 无此检查。上游注释自述为 `PrivateDecl.ti` 符号名不含文件名的
     * 临时 workaround（"It should be remove when bug of PrivateDecl.ti is fixed"）。
     * cjc 1.0.0 / 1.0.5 双 SDK 实测确认该分界：v1.0.0 下同名顶层 private 零诊断，
     * v1.0.5 起报 `currently, it is not possible to export two private declarations
     * with the same name`（锚点为首个声明的 identifier，后续声明附 note）。
     * 证据与探针见 `cfir/analysis-tests/build/linkage-private-dup-analysis-20260921.md` §3.2/§3.2b。
     */
    ExportSamePrivateDeclCheck(
        LanguageVersion.CANGJIE_1_0_2,
        behaviorAfterSinceVersion = LanguageFeatureBehaviorAfterSinceVersion.CanStillBeDisabledForNow(NO_ISSUE_SPECIFIED),
    ),

    /**
     * 允许类型推断将互斥下界约束收敛出的交集类型作为固定结果。
     *
     * 默认全版本关闭（sinceVersion = null）：关闭时对齐官方 cjc 行为，当多个下界
     * 约束无法收敛到单一具体类型（例如 `choose(1, true)` 产生 `Hashable & ToString`
     * 交集候选）时推断失败并报告 UNABLE_TO_INFER_GENERIC_FUNC；开启后保持 Kotlin
     * K2 兼容的推断行为，交集候选继续按现状近似为公共父类型完成推断。
     */
    AllowIntersectionTypesInInference(null),
    ;

    /**
     * 与 Kotlin `LanguageFeature` 相同的元模型不变量：test-only 特性不能
     * 同时声明一个会按语言版本默认启用的版本。否则旧版本配置和命令行
     * test-only 语义会互相矛盾，消费者无法区分“默认能力”和“仅测试能力”。
     */
    init {
        if (testOnly && sinceVersion != null) {
            error("$this: test-only feature must not have a default since version $sinceVersion")
        }
    }

    /** 面向诊断和 IDE 的可读特性名称，不暴露 Kotlin 枚举命名约定。 */
    val presentableName: String
        get() = name
            .split("(?<!^)(?=[A-Z])".toRegex())
            .joinToString(separator = " ", transform = String::lowercase)

    /** 包含可选提示链接的可读特性文本。 */
    val presentableText: String
        get() = if (hintUrl == null) presentableName else "$presentableName (See: $hintUrl)"

    /**
     * progressive 模式可自动开启的状态计算。
     *
     * 本项目当前没有 Kotlin CLI 的 progressive 配置入口，因此这里只提供
     * 与 Kotlin 相同的无副作用元数据投影，不在任何 session 中隐式开启特性。
     */
    val actuallyEnabledInProgressiveMode: Boolean
        get() = enabledInProgressiveMode && sinceVersion != null

    enum class State(override val description: String) : DescriptionAware {
        ENABLED("Enabled"),
        DISABLED("Disabled");
    }

    companion object {
        /**
         * 按 Kotlin `LanguageFeature.fromString` 契约按精确枚举名查找语言特性。
         * 命令行解析器负责把外部输入规范化；公共身份查找不能把不同拼写
         * 合并成同一个 feature。
         */
        fun fromName(name: String): LanguageFeature? {
            return entries.firstOrNull { it.name == name }
        }
    }
}

/**
 * 诊断或语言特性警告的处理级别。
 */
enum class WarningLevel {
    Error,
    Warning,
    Disabled,
}

/**
 * 分析阶段使用的类型安全配置键。
 *
 * 每个 flag 通过名称建立身份，携带默认值，并由 [LanguageVersionSettings] 在查询时完成类型恢复。
 */
class AnalysisFlag<out T> internal constructor(
    /**
     * 配置键的稳定名称，通常来自声明属性名。
     */
    private val name: String,
    /**
     * 设置中未显式指定该键时返回的默认值。
     */
    val defaultValue: T
) {
    /**
     * 根据配置键名称比较两个分析开关是否表示同一项配置。
     */
    override fun equals(other: Any?): Boolean = other is AnalysisFlag<*> && other.name == name

    /**
     * 返回基于配置键名称的哈希值。
     */
    override fun hashCode(): Int = name.hashCode()

    /**
     * 返回配置键名称，便于诊断、日志和调试输出。
     */
    override fun toString(): String = name

    /**
     * 将属性声明转换为稳定的 [AnalysisFlag] 实例。
     */
    class Delegate<out T>(name: String, defaultValue: T) : ReadOnlyProperty<Any?, AnalysisFlag<T>> {
        /**
         * 该委托暴露的实际分析开关实例。
         */
        private val flag = AnalysisFlag(name, defaultValue)

        /**
         * 返回已创建的分析开关，保证同一属性访问始终得到同一配置键。
         */
        override fun getValue(thisRef: Any?, property: KProperty<*>): AnalysisFlag<T> = flag
    }

    /**
     * 构造常用分析开关委托的命名空间。
     */
    object Delegates {
        /**
         * 布尔分析开关委托，默认值可由声明处覆盖。
         */
        open class Boolean(val defaultValue: kotlin.Boolean) {
            /**
             * 默认值为 false 的布尔开关委托。
             */
            companion object : Boolean(defaultValue = false)

            /**
             * 根据属性名创建布尔分析开关。
             */
            operator fun provideDelegate(instance: Any?, property: KProperty<*>) = Delegate(property.name, defaultValue)
        }

        /**
         * 警告级别映射开关委托，用于按诊断名称覆盖 warning/error/disabled。
         */
        object WarningLevelMap {
            /**
             * 根据属性名创建警告级别映射分析开关。
             */
            operator fun provideDelegate(
                instance: Any?,
                property: KProperty<*>
            ): Delegate<Map<String, WarningLevel>> = Delegate(property.name, emptyMap())
        }


    }
}

/**
 * 编译器分析阶段可读取的全局分析开关集合。
 */
object AnalysisFlags {
    /**
     * 按诊断或功能名配置的警告级别覆盖表。
     */
    val warningLevels by AnalysisFlag.Delegates.WarningLevelMap

    /**
     * IDE 模式开关，用于启用交互式分析所需的容错路径。
     */
    @JvmStatic
    val ideMode by AnalysisFlag.Delegates.Boolean


    /**
     * 是否处于标准库自身编译模式。
     */
    @JvmStatic
    val stdlibCompilation by AnalysisFlag.Delegates.Boolean

    /**
     * 是否跳过预导入和前置标准定义。
     */
    @JvmStatic
    val noPrelude by AnalysisFlag.Delegates.Boolean

    /**
     * 控制类型解析阶段是否自动展开 type alias。
     *
     * 对齐 Kotlin `AnalysisFlags.expandTypeAliasesInTypeResolution`：
     * 默认开启，仅测试环境会显式关闭，用于覆盖 without-alias-expansion 真实前端路径。
     */
    @JvmStatic
    val expandTypeAliasesInTypeResolution by AnalysisFlag.Delegates.Boolean(defaultValue = true)

}

/**
 * 语言版本设置的统一查询接口。
 *
 * 承载两组正交配置：语言特性开关（[getFeatureSupport]/[supportsFeature]）、
 * 分析标志（[getFlag]）、以及版本事实（[languageVersion]）。
 *
 * 注意：不要通过 [languageVersion] 直接开合具体特性或检查——
 * 应新增 [LanguageFeature] 枚举项并走 [supportsFeature] 查询。
 */
interface LanguageVersionSettings {
    /** 查询特性状态：显式配置优先，否则按版本推导默认值。 */
    fun getFeatureSupport(feature: LanguageFeature): LanguageFeature.State

    /** 判断特性是否启用；等价于状态为 [LanguageFeature.State.ENABLED]。 */
    fun supportsFeature(feature: LanguageFeature): Boolean =
        getFeatureSupport(feature) == LanguageFeature.State.ENABLED

    /** 返回显式定制过的特性开关集合（不含按版本推导的默认项）。 */
    fun getCustomizedLanguageFeatures(): Map<LanguageFeature, LanguageFeature.State>

    /** 当前编译是否为预发布状态：语言版本预发布，或启用了强制预发布二进制的特性。 */
    fun isPreRelease(): Boolean

    /** 读取分析标志值；未配置时返回该标志的默认值。 */
    fun <T> getFlag(flag: AnalysisFlag<T>): T

    // Please do not use this to enable/disable specific features/checks. Instead add a new LanguageFeature entry and call supportsFeature
    val languageVersion: LanguageVersion

    companion object {
        /** 环境变量配置白名单资源名；仅存在该资源时才允许从环境读取设置。 */
        const val RESOURCE_NAME_TO_ALLOW_READING_FROM_ENVIRONMENT = "META-INF/allow-configuring-from-environment"
    }
}

/**
 * 语言特性门禁的统一结果。
 *
 * 版本比较只允许在这里进行；annotation、checker、resolve 和诊断 renderer
 * 只能消费这个结果或 [LanguageVersionSettings.supportsFeature]，不得各自复制
 * `sinceVersion` 的判断。
 */
enum class LanguageFeatureSupportStatus {
    SUPPORTED,
    UNSUPPORTED_LANGUAGE_VERSION,
    DISABLED,
    EXPERIMENTAL,
}

/**
 * 计算一个 feature 在当前设置中的门禁原因。
 *
 * 显式 ENABLED 状态优先于默认版本推导，与 Kotlin 的
 * [LanguageVersionSettingsImpl.getFeatureSupport] 保持相同优先级；显式
 * DISABLED 仍要先经过版本原因判断，以便诊断不丢失“尚未可用”的事实。
 */
fun LanguageVersionSettings.featureSupportStatus(
    feature: LanguageFeature,
): LanguageFeatureSupportStatus {
    val explicitState = getCustomizedLanguageFeatures()[feature]
    // Kotlin keeps an explicit ENABLED override authoritative even when the
    // requested language/API version predates the feature.  This is the same
    // contract as LanguageVersionSettingsImpl.getFeatureSupport.  An explicit
    // DISABLED state is handled after the version checks below so diagnostics
    // preserve an older language/API version as the primary reason.
    if (explicitState == LanguageFeature.State.ENABLED) {
        return LanguageFeatureSupportStatus.SUPPORTED
    }

    // The version reason must win over an explicit DISABLED state.  Otherwise
    // `- LANGUAGE_VERSION` (or an equivalent test setting) is rendered as
    // merely "disabled", losing the fact that the construct is unavailable
    // in that language/API version.
    val sinceVersion = feature.sinceVersion
    if (sinceVersion != null && languageVersion < sinceVersion) {
        return LanguageFeatureSupportStatus.UNSUPPORTED_LANGUAGE_VERSION
    }
    // An experimental feature has no since-language version.  Keep this
    // distinction, matching Kotlin's renderer.  An
    // explicit ENABLED state was handled above and is already supported;
    // reaching this branch therefore means the feature is still experimental
    // (or explicitly disabled) rather than that an explicit enable should be
    // rejected by the status layer.
    if (sinceVersion == null) {
        return if (explicitState == LanguageFeature.State.DISABLED) {
            LanguageFeatureSupportStatus.DISABLED
        } else {
            LanguageFeatureSupportStatus.EXPERIMENTAL
        }
    }
    if (explicitState == LanguageFeature.State.DISABLED) {
        return LanguageFeatureSupportStatus.DISABLED
    }
    return if (supportsFeature(feature)) {
        LanguageFeatureSupportStatus.SUPPORTED
    } else {
        LanguageFeatureSupportStatus.UNSUPPORTED_LANGUAGE_VERSION
    }
}

/**
 * 统一 feature gate 的布尔入口；需要诊断原因时使用 [featureSupportStatus]。
 */
fun LanguageVersionSettings.requireFeatureSupport(feature: LanguageFeature): Boolean =
    featureSupportStatus(feature) == LanguageFeatureSupportStatus.SUPPORTED

/** Kotlin KLIB 对位：返回显式开启且尚未按版本默认开启的 feature。 */
fun LanguageVersionSettings.getCustomizedEffectivelyEnabledLanguageFeatures(): Set<LanguageFeature> =
    getCustomizedLanguageFeatures().entries.mapNotNullTo(mutableSetOf()) { (feature, state) ->
        feature.takeIf { !isEnabledByDefault(feature) && state == LanguageFeature.State.ENABLED }
    }

/** Kotlin KLIB 对位：返回显式关闭但按版本本应默认开启的 feature。 */
fun LanguageVersionSettings.getCustomizedEffectivelyDisabledLanguageFeatures(): Set<LanguageFeature> =
    getCustomizedLanguageFeatures().entries.mapNotNullTo(mutableSetOf()) { (feature, state) ->
        feature.takeIf {
            isEnabledByDefault(feature) &&
                state == LanguageFeature.State.DISABLED
        }
    }

/**
 * [LanguageVersionSettings] 的不可变实现。
 *
 * 构造时即把传入的标志与特性映射包为只读；特性查询先查显式定制，
 * 再按版本边界推导默认状态。
 *
 * @property languageVersion 当前语言版本。
 * @param analysisFlags 分析标志覆盖表；缺省项回落到各标志的默认值。
 * @param specificFeatures 显式特性开关，优先级高于按版本推导。
 */
class LanguageVersionSettingsImpl @JvmOverloads constructor(
    override val languageVersion: LanguageVersion,
    analysisFlags: Map<AnalysisFlag<*>, Any?> = emptyMap(),
    specificFeatures: Map<LanguageFeature, LanguageFeature.State> = emptyMap()
) : LanguageVersionSettings {
    private val analysisFlags: Map<AnalysisFlag<*>, *> = Collections.unmodifiableMap(analysisFlags)
    private val specificFeatures: Map<LanguageFeature, LanguageFeature.State> =
        Collections.unmodifiableMap(specificFeatures)

    override fun getFeatureSupport(feature: LanguageFeature): LanguageFeature.State {
        // This is intentionally a direct explicit override, exactly like
        // Kotlin's LanguageVersionSettingsImpl.  The lifecycle policy is a
        // CLI/configuration validation concern; silently changing the state
        // here would make all resolver/checker consumers observe a value the
        // user did not request and would lose the correct diagnostic reason.
        specificFeatures[feature]?.let { return it }

        return if (isEnabledByDefault(feature)) {
            LanguageFeature.State.ENABLED
        } else {
            LanguageFeature.State.DISABLED
        }
    }

    override fun getCustomizedLanguageFeatures(): Map<LanguageFeature, LanguageFeature.State> = specificFeatures


    @Suppress("UNCHECKED_CAST")
    override fun <T> getFlag(flag: AnalysisFlag<T>): T = analysisFlags[flag] as T? ?: flag.defaultValue

    override fun isPreRelease(): Boolean = languageVersion.isPreRelease() ||
            specificFeatures.any { (feature, state) ->
                state == LanguageFeature.State.ENABLED && feature.forcesPreReleaseBinariesIfEnabled()
            }

    override fun toString(): String = buildString {
        append("Language = $languageVersion")
        specificFeatures.entries.sortedBy { (feature, _) -> feature.ordinal }.forEach { (feature, state) ->
            val marker = when (state) {
                LanguageFeature.State.ENABLED -> '+'
                LanguageFeature.State.DISABLED -> '-'
            }
            append(" $marker$feature")
        }
        analysisFlags.entries.sortedBy { (flag, _) -> flag.toString() }.forEach { (flag, value) ->
            append(" $flag:$value")
        }
    }

    companion object {
        @JvmField
        val DEFAULT = LanguageVersionSettingsImpl(LanguageVersion.LATEST_STABLE)
    }
}

/** 判断语言版本本身是否为预发布：非稳定版，或带预发布标签的最新稳定版。 */
fun LanguageVersion.isPreRelease(): Boolean {
    if (!isStable) return true

    return this.preReleaseTag != null && this == LanguageVersion.LATEST_STABLE
}

/** 判断特性启用后是否强制产物为预发布二进制：特性尚未随稳定版发布且声明了该要求。 */
fun LanguageFeature.forcesPreReleaseBinariesIfEnabled(): Boolean {
    val isFeatureNotReleasedYet = sinceVersion?.isStable != true
    return isFeatureNotReleasedYet && forcesPreReleaseBinaries
}

/** 推导特性的默认启用状态：since 版本已到达即默认启用。 */
fun LanguageVersionSettings.isEnabledByDefault(languageFeature: LanguageFeature): Boolean =
    languageFeature.sinceVersion != null && languageVersion >= languageFeature.sinceVersion
