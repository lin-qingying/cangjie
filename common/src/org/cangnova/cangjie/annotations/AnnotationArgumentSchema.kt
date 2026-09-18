package org.cangnova.cangjie.annotations

import org.cangnova.cangjie.LanguageFeature

/** 参数语义种类；值参数绑定与常量求值由 CFIR resolve 负责。 */
public enum class AnnotationParameterKind { STRING, BOOLEAN, INTEGER, EXPRESSION, REFERENCE, TARGET_ARRAY }

/** 注解参数的常量默认值，公共模型不引用 PSI 或 CFIR 节点。 */
public sealed interface AnnotationDefaultValue {
    public data class StringValue(val value: String) : AnnotationDefaultValue
    public data class BooleanValue(val value: Boolean) : AnnotationDefaultValue
}
/** 一个内置参数的名字、实参形式和默认值。 */
public data class AnnotationParameterSchema(
    val name: String,
    val kind: AnnotationParameterKind,
    val required: Boolean = false,
    val acceptsPositional: Boolean = false,
    val defaultValue: AnnotationDefaultValue? = null,
)

/**
 * 单一注解参数契约。
 *
 * 特殊语法（When/Attribute/Overflow）保留自己的语法 owner，不能伪装为普通构造器。
 * 自定义系统类注解使用真实 constructor；这里只描述其语义消费者关心的参数。
 */
public data class AnnotationArgumentSchema(
    val parameters: List<AnnotationParameterSchema> = emptyList(),
    val variadic: Boolean = false,
    val acceptsArbitrarySingleName: Boolean = false,
) {
    public val positionalParameter: AnnotationParameterSchema?
        get() = parameters.singleOrNull { it.acceptsPositional }

    public companion object {
        public val NONE: AnnotationArgumentSchema = AnnotationArgumentSchema()
    }
}

/** 内置注解所属的语义领域。 */
public enum class BuiltInAnnotationCategory { FFI, COMPILER_DIRECTIVE, META, SEMANTIC, TESTING, SYSTEM }

/** 注册表到语义 owner 的稳定分派标识，不在 common 中持有编译器实现。 */
public enum class AnnotationSemanticHandler {
    C_FFI, JAVA_FFI, OBJC_FFI, FOREIGN_NAME, ANNOTATION_TYPE, DEPRECATION,
    OVERFLOW, CONDITIONAL_COMPILATION, ATTRIBUTES, INTRINSIC, CONST_EVALUATION,
    MOCK_PREPARATION, PACKAGE_PRODUCT, AVAILABILITY,
}

/** language builtin 与系统类注解共享的静态查询协议。 */
public sealed interface AnnotationDescriptor {
    public val sourceName: String
    public val kind: BuiltInAnnotationKind?
    public val origin: CangjieAnnotationOrigin
    public val category: BuiltInAnnotationCategory
    public val argumentSyntax: CangjieAnnotationArgumentSyntax
    public val argumentSchema: AnnotationArgumentSchema
    public val declarationTargets: Set<CangjieAnnotationTarget>
    public val repeatable: Boolean
    public val supportsCompileTimeVisibleForm: Boolean
    public val semanticHandler: AnnotationSemanticHandler
    /** Language feature which introduced this annotation surface. */
    public val requiredLanguageFeature: LanguageFeature? get() = null
    public val overflowStrategy: CangjieOverflowStrategy? get() = null
}

/** 官方 AnnotationKind 的静态契约；重复规则按源码名称而非 kind 判定。 */
public data class BuiltInAnnotationDescriptor(
    override val sourceName: String,
    override val kind: BuiltInAnnotationKind?,
    override val category: BuiltInAnnotationCategory,
    override val argumentSyntax: CangjieAnnotationArgumentSyntax,
    override val argumentSchema: AnnotationArgumentSchema,
    override val declarationTargets: Set<CangjieAnnotationTarget>,
    override val semanticHandler: AnnotationSemanticHandler,
    override val overflowStrategy: CangjieOverflowStrategy? = null,
    val standardLibraryOnly: Boolean = false,
    val hasSourceParserEntry: Boolean = true,
    val allowsExpression: Boolean = false,
    val descriptorOrigin: CangjieAnnotationOrigin = CangjieAnnotationOrigin.LANGUAGE_BUILT_IN,
    override val requiredLanguageFeature: LanguageFeature? = null,
) : AnnotationDescriptor {
    override val origin: CangjieAnnotationOrigin get() = descriptorOrigin
    override val repeatable: Boolean get() = false
    override val supportsCompileTimeVisibleForm: Boolean get() = false
}

/** 系统注解按真实 class id 识别；IfAvailable 由独立表达式处理器解析。 */
public data class SystemAnnotationDescriptor(
    override val sourceName: String,
    val classFqName: org.cangnova.cangjie.name.FqName?,
    override val argumentSyntax: CangjieAnnotationArgumentSyntax,
    override val argumentSchema: AnnotationArgumentSchema,
    override val declarationTargets: Set<CangjieAnnotationTarget>,
    override val repeatable: Boolean,
    override val supportsCompileTimeVisibleForm: Boolean,
    override val origin: CangjieAnnotationOrigin = CangjieAnnotationOrigin.SYSTEM_MACRO,
    override val requiredLanguageFeature: LanguageFeature? = null,
) : AnnotationDescriptor {
    override val kind: BuiltInAnnotationKind? get() = null
    override val category: BuiltInAnnotationCategory get() = BuiltInAnnotationCategory.SYSTEM
    override val semanticHandler: AnnotationSemanticHandler get() = AnnotationSemanticHandler.AVAILABILITY
}
