package org.cangnova.cangjie.arguments.dsl.base

import org.cangnova.cangjie.arguments.dsl.types.CangJieArgumentValueType
import org.cangnova.cangjie.arguments.dsl.types.StringType
import kotlin.properties.ReadOnlyProperty

/** 单字符短选项的附加值语法。 */
public sealed interface CangJieShortOptionValueMode {
    /** 值必须紧跟短选项，例如 `-O2`；不读取下一个独立参数。 */
    public data object AttachedRequired : CangJieShortOptionValueMode

    /** 值可紧跟短选项；短选项单独出现时使用声明的值。 */
    public data class AttachedOptional(
        /** 短选项单独出现时解析出的值。 */
        val valueWhenOmitted: String,
    ) : CangJieShortOptionValueMode
}

/**
 * 单个编译器命令行参数的完整元数据模型。
 */
@OptIn(ExperimentalArgumentApi::class)
data class CangJieCompilerArgument(
    /**
     * 参数长名称，不包含命令行前缀。
     */
    val name: String,
    /**
     * 参数短名称，不存在短参数时为空。
     */
    val shortName: String? = null,
    /**
     * 单字符短选项的黏连值语法；未设置时保留标准的独立参数值解析。
     */
    val shortOptionValueMode: CangJieShortOptionValueMode? = null,
    /** SEPARATED 参数的原生 alias 可直接黏连值，也可使用等号；不改变连续短参数的规则。 */
    val acceptsJoinedAliasValues: Boolean = false,
    /** 单次输入值的精确合法集合；空集合表示此参数没有枚举限制。 */
    val allowedValues: Set<String> = emptySet(),
    /**
     * 已废弃但仍需要识别或生成兼容信息的旧参数名称。
     */
    val deprecatedName: String? = null,
    /**
     * 随版本演进的参数说明文本。
     */
    val description: ReleaseDependent<String>,
    /**
     * 参数名与参数值之间使用的分隔符策略。
     */
    val delimiter: Delimiter?,
    /**
     * 参数值在 DSL 中声明的主值类型。
     */
    val valueType: CangJieArgumentValueType<*>,
    /**
     * 参数值占位符或补充描述，允许按版本提供不同文本。
     */
    val valueDescription: ReleaseDependent<String?> = null.asReleaseDependent(),
    /**
     * 参数从引入到稳定、废弃、移除的版本生命周期。
     */
    override val releaseVersionsMetadata: CangJieReleaseVersionLifecycle,

    /**
     * 生成到编译器参数类时使用的实际属性类型；默认与 `valueType` 相同。
     */
    val argumentType: CangJieArgumentValueType<*> = valueType,
    /**
     * 需要附加到生成参数属性上的注解集合。
     */
    val additionalAnnotations: List<Annotation> = emptyList(),
    /**
     * 编译器内部使用的参数名；为空时使用公开参数名。
     */
    val compilerName: String? = null,
    /**
     * 标记该参数是否已经过时，不再作为推荐的公开参数。
     */
    val isObsolete: Boolean = false,
) : WithCangJieReleaseVersionsMetadata {

    init {
        if (acceptsJoinedAliasValues) {
            require(!shortName.isNullOrBlank()) { "Joined alias values require an actual option alias" }
            require(shortOptionValueMode == null) { "Separated alias values cannot use continuous short-option rules" }
        }
        if (shortOptionValueMode != null) {
            require((shortName ?: name).length == 1) {
                "Attached short-option values require a single-character option name"
            }
            require(valueType is StringType && argumentType is StringType) {
                "Attached short-option values require string argument types"
            }
        }
    }

    /**
     * 命令行参数值的分隔符描述。
     */
    enum class Delimiter(val constantName: String) {
        /**
         * 使用当前后端或生成器的默认分隔符。
         */
        Default("default"),

        /**
         * 参数名与参数值之间不使用额外分隔符。
         */
        None("none"),

        /**
         * 使用当前平台路径分隔符连接多个路径值。
         */
        PathSeparator("path-separator"),

        /**
         * 使用空格分隔参数名和值。
         */
        Space("space"),

        /**
         * 使用分号连接多个值。
         */
        Semicolon("semicolon"),
    }
}

/** 参数描述对应的生成参数类属性名，供字段生成与命令行解析共享。 */
public val CangJieCompilerArgument.generatedPropertyName: String
    get() = compilerName ?: name
        .removePrefix("X").removePrefix("X")
        .split("-").joinToString("") { it.replaceFirstChar(Char::uppercaseChar) }
        .replaceFirstChar(Char::lowercaseChar)

/**
 * 构造 `CangJieCompilerArgument` 的 DSL builder。
 */
@ExperimentalArgumentApi
@CangJieArgumentsDslMarker
class CangJieCompilerArgumentBuilder {
    /**
     * 待构造参数的长名称。
     */
    lateinit var name: String

    /**
     * 待构造参数的短名称。
     */
    var shortName: String? = null

    /** 单字符短选项的黏连值语法。 */
    var shortOptionValueMode: CangJieShortOptionValueMode? = null

    /** 原生 SEPARATED alias 的黏连值及等号语法。 */
    var acceptsJoinedAliasValues: Boolean = false

    /** 参数单次输入值的精确合法集合。 */
    var allowedValues: Set<String> = emptySet()

    /**
     * 待构造参数的废弃旧名称。
     */
    var deprecatedName: String? = null

    /**
     * 待构造参数的版本化说明文本。
     */
    lateinit var description: ReleaseDependent<String>

    /**
     * 待构造参数的值类型。
     */
    lateinit var valueType: CangJieArgumentValueType<*>

    /**
     * 待构造参数的版本化值描述。
     */
    var valueDescription: ReleaseDependent<String?> = null.asReleaseDependent()

    /**
     * 生成参数类时使用的实际属性类型；为空时回退到 `valueType`。
     */
    var argumentType: CangJieArgumentValueType<*>? = null

    /**
     * 编译器内部名称覆盖值。
     */
    var compilerName: String? = null

    /**
     * 命令行参数值分隔符策略。
     */
    var delimiter: CangJieCompilerArgument.Delimiter? = null

    /**
     * builder 中收集到的参数生命周期元数据。
     */
    private lateinit var releaseVersionsMetadata: CangJieReleaseVersionLifecycle

    /**
     * builder 中收集到的生成属性附加注解。
     */
    private val additionalAnnotations: MutableList<Annotation> = mutableListOf()

    /**
     * 声明参数在各个仓颉版本中的生命周期节点。
     */
    fun lifecycle(
        introducedVersion: CangJieReleaseVersion,
        stabilizedVersion: CangJieReleaseVersion? = null,
        deprecatedVersion: CangJieReleaseVersion? = null,
        removedVersion: CangJieReleaseVersion? = null,
    ) {
        releaseVersionsMetadata = CangJieReleaseVersionLifecycle(
            introducedVersion,
            stabilizedVersion,
            deprecatedVersion,
            removedVersion
        )
    }

    /**
     * 追加生成参数属性时需要保留的注解。
     */
    fun additionalAnnotations(vararg annotation: Annotation) {
        additionalAnnotations.addAll(annotation)
    }

    /**
     * 将 builder 当前状态冻结为不可变参数元数据。
     */
    @OptIn(ExperimentalArgumentApi::class)
    fun build(): CangJieCompilerArgument = CangJieCompilerArgument(
        name = name,
        shortName = shortName,
        shortOptionValueMode = shortOptionValueMode,
        acceptsJoinedAliasValues = acceptsJoinedAliasValues,
        allowedValues = allowedValues.toSet(),
        deprecatedName = deprecatedName,
        description = description,
        valueType = valueType,
        valueDescription = valueDescription,
        argumentType = argumentType ?: valueType,
        releaseVersionsMetadata = releaseVersionsMetadata,
        additionalAnnotations = additionalAnnotations,
        compilerName = compilerName,
        delimiter = delimiter,
    )
}

/**
 * 创建可作为 DSL 委托属性使用的编译器参数定义。
 */
@ExperimentalArgumentApi
fun compilerArgument(
    config: CangJieCompilerArgumentBuilder.() -> Unit,
) = ReadOnlyProperty<Any?, CangJieCompilerArgument> { _, _ ->
    val builder = CangJieCompilerArgumentBuilder()
    config(builder)
    builder.build()
}
