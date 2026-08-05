package org.cangnova.cangjie

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * 表示编译器理解的仓颉语言版本号。
 *
 * 版本按 `major.minor.patch` 三段比较，用于语言特性开关、诊断降级和兼容性判断。
 */
data class LanguageVersion(
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
) : Comparable<LanguageVersion> {
    /**
     * 按主版本、次版本、修订版本顺序比较两个语言版本。
     */
    override fun compareTo(other: LanguageVersion): Int {
        if (major != other.major) return major.compareTo(other.major)
        if (minor != other.minor) return minor.compareTo(other.minor)
        return patch.compareTo(other.patch)
    }

    /**
     * 渲染为配置文件和命令行使用的三段式版本字符串。
     */
    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        /**
         * 从 `major.minor.patch` 格式解析语言版本，格式或数字非法时返回 null。
         */
        fun parse(value: String): LanguageVersion? {
            val parts = value.split('.')
            if (parts.size != 3) return null

            val major = parts[0].toIntOrNull() ?: return null
            val minor = parts[1].toIntOrNull() ?: return null
            val patch = parts[2].toIntOrNull() ?: return null
            return LanguageVersion(major, minor, patch)
        }
    }
}

/**
 * 当前编译器内置的语言版本常量。
 */
object LanguageVersions {
    /**
     * 仓颉 1.0.5 语言版本。
     */
    val V_1_0_5: LanguageVersion = LanguageVersion(1, 0, 5)
    val V_1_1_0: LanguageVersion = LanguageVersion(1, 1, 0)
    val V_1_1_3: LanguageVersion = LanguageVersion(1, 1, 3)

    /**
     * 默认面向用户启用的最新稳定语言版本。
     */
    val LATEST_STABLE: LanguageVersion = V_1_0_5
}

/**
 * 可由语言版本设置显式开启的语言特性。
 *
 * 这些开关用于在同一编译器中承载实验特性、兼容行为和诊断策略差异。
 */
enum class LanguageFeature {
    /**
     * Enables data-flow-analysis based warnings in frontend diagnostics.
     */
    EnableDfaWarnings,
    /**
     * Reports lambda/function value mismatch as ARGUMENT_TYPE_MISMATCH instead of
     * RETURN_TYPE_MISMATCH on lambda body return expression.
     */
    LambdaReturnTypeMismatchAsArgumentTypeMismatch,
    InvalidBinaryOperatorDiagnostics,
    LexicographicVariableReadinessCalculation,
    EffectHandlers,
    ;

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
            operator fun provideDelegate(instance: Any?, property: KProperty<*>):  AnalysisFlag.Delegate<Map<String, WarningLevel>> = Delegate(property.name, emptyMap())
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
     * 是否允许源码使用 Kotlin 包名兼容路径。
     */
    @JvmStatic
    val allowKotlinPackage by AnalysisFlag.Delegates.Boolean
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
 * 单次分析使用的语言版本、启用特性和分析开关集合。
 */
data class LanguageVersionSettings(
    /**
     * 当前分析会话采用的语言版本。
     */
    val languageVersion: LanguageVersion = LanguageVersions.LATEST_STABLE,
    /**
     * 在当前语言版本之外额外启用的语言特性集合。
     */
    val enabledFeatures: Set<LanguageFeature> = emptySet(),
    /**
     * 以 [AnalysisFlag] 为键的分析配置覆盖值。
     */
    val analysisFlags: Map<AnalysisFlag<*>, Any?> = emptyMap(),
) {
    /**
     * 判断当前设置是否启用了指定语言特性。
     */
    fun supportsFeature(feature: LanguageFeature): Boolean = feature in enabledFeatures

    /**
     * 读取指定分析开关的值；未配置时返回该开关声明的默认值。
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getFlag(flag: AnalysisFlag<T>): T {
        return analysisFlags[flag] as? T ?: flag.defaultValue
    }

    companion object {
        /**
         * 面向普通编译场景的默认语言版本设置。
         */
        val DEFAULT: LanguageVersionSettings = LanguageVersionSettings()
    }
}
