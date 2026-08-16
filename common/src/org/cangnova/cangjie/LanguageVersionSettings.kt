package org.cangnova.cangjie

import org.cangnova.cangjie.config.ApiVersion
import org.cangnova.cangjie.util.DescriptionAware
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import java.util.*

interface LanguageOrApiVersion : DescriptionAware {
    val versionString: String

    val isStable: Boolean

    val isDeprecated: Boolean

    val isUnsupported: Boolean

    override val description: String
        get() = when {
            !isStable -> "$versionString (experimental)"
            isDeprecated -> "$versionString (deprecated)"
            isUnsupported -> "$versionString (unsupported)"
            else -> versionString
        }
}

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
) : DescriptionAware, LanguageOrApiVersion {

    CANGJIE_1_0_0(1, 0, 0),
    CANGJIE_1_0_5(1, 0, 5),
    CANGJIE_1_1_0(1, 1, 0),
    CANGJIE_1_1_3(1, 1, 3);


    override val isStable: Boolean
        get() = this <= LATEST_STABLE


    override val isDeprecated: Boolean
        get() = this in FIRST_SUPPORTED..<FIRST_NON_DEPRECATED

    override val isUnsupported: Boolean
        get() = this < FIRST_SUPPORTED

    override val versionString: String = "$major.$minor"

    override fun toString() = versionString

    companion object {
        fun parse(versionString: String): LanguageVersion {
            val parts = versionString.split('.')
            if (parts.size < 2) error("Invalid version string: $versionString")
            val major = parts[0].toIntOrNull() ?: error("Invalid major version: ${parts[0]}")
            val minor = parts[1].toIntOrNull() ?: error("Invalid minor version: ${parts[1]}")
            val patch = if (parts.size > 2) parts[2].toIntOrNull() ?: error("Invalid patch version: ${parts[2]}") else 0
            return entries.firstOrNull { it.major == major && it.minor == minor && it.patch == patch }
                ?: error("Version not found: $versionString")
        }

        @JvmField
        val FIRST_SUPPORTED = CANGJIE_1_0_0

        @JvmField
        val FIRST_API_SUPPORTED = CANGJIE_1_0_0

        @JvmField
        val FIRST_NON_DEPRECATED = CANGJIE_1_0_0

        @JvmField
        val LATEST_STABLE = CANGJIE_1_0_5

    }
}

sealed class LanguageFeatureBehaviorAfterSinceVersion {
    data object CannotBeDisabled : LanguageFeatureBehaviorAfterSinceVersion()
    data class CanStillBeDisabledForNow(val relevantTicketId: String) : LanguageFeatureBehaviorAfterSinceVersion()
}

const val NO_ISSUE_SPECIFIED = "No issue"

/**
 * 可由语言版本设置显式开启的语言特性。
 *
 * 这些开关用于在同一编译器中承载实验特性、兼容行为和诊断策略差异。
 */
enum class LanguageFeature(
    val sinceVersion: LanguageVersion?,
    val sinceApiVersion: ApiVersion = ApiVersion.CANGJIE_1_0_0,
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
    EffectHandlers(LanguageVersion.CANGJIE_1_0_0),
    ;

    enum class State(override val description: String) : DescriptionAware {
        ENABLED("Enabled"),
        DISABLED("Disabled");
    }

    companion object {
        /**
         * 按名称查找语言特性，忽略大小写以兼容命令行和测试指令输入。
         */
        fun fromName(name: String): LanguageFeature? {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
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
            ): AnalysisFlag.Delegate<Map<String, WarningLevel>> = Delegate(property.name, emptyMap())
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

interface LanguageVersionSettings {
    fun getFeatureSupport(feature: LanguageFeature): LanguageFeature.State

    fun supportsFeature(feature: LanguageFeature): Boolean =
        getFeatureSupport(feature) == LanguageFeature.State.ENABLED

    fun getCustomizedLanguageFeatures(): Map<LanguageFeature, LanguageFeature.State>

    fun isPreRelease(): Boolean

    fun <T> getFlag(flag: AnalysisFlag<T>): T

    val apiVersion: ApiVersion

    // Please do not use this to enable/disable specific features/checks. Instead add a new LanguageFeature entry and call supportsFeature
    val languageVersion: LanguageVersion

    companion object {
        const val RESOURCE_NAME_TO_ALLOW_READING_FROM_ENVIRONMENT = "META-INF/allow-configuring-from-environment"
    }
}

class LanguageVersionSettingsImpl @JvmOverloads constructor(
    override val languageVersion: LanguageVersion,
    override val apiVersion: ApiVersion,
    analysisFlags: Map<AnalysisFlag<*>, Any?> = emptyMap(),
    specificFeatures: Map<LanguageFeature, LanguageFeature.State> = emptyMap()
) : LanguageVersionSettings {
    private val analysisFlags: Map<AnalysisFlag<*>, *> = Collections.unmodifiableMap(analysisFlags)
    private val specificFeatures: Map<LanguageFeature, LanguageFeature.State> =
        Collections.unmodifiableMap(specificFeatures)

    override fun getFeatureSupport(feature: LanguageFeature): LanguageFeature.State {
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

    companion object {
        @JvmField
        val DEFAULT = LanguageVersionSettingsImpl(LanguageVersion.LATEST_STABLE, ApiVersion.LATEST_STABLE)
    }
}

fun LanguageVersion.isPreRelease(): Boolean {
    if (!isStable) return true

    return this.preReleaseTag != null && this == LanguageVersion.LATEST_STABLE
}

fun LanguageFeature.forcesPreReleaseBinariesIfEnabled(): Boolean {
    val isFeatureNotReleasedYet = sinceVersion?.isStable != true
    return isFeatureNotReleasedYet && forcesPreReleaseBinaries
}

fun LanguageVersionSettings.isEnabledByDefault(languageFeature: LanguageFeature): Boolean =
    languageFeature.sinceVersion != null && languageVersion >= languageFeature.sinceVersion && apiVersion >= languageFeature.sinceApiVersion
