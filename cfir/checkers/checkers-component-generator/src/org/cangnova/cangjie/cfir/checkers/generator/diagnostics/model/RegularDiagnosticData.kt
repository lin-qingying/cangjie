package org.cangnova.cangjie.cfir.checkers.generator.diagnostics.model

import org.cangnova.cangjie.LanguageFeature
import org.cangnova.cangjie.cfir.diagnostics.Severity
import kotlin.reflect.KType

/**
 * 诊断生成器使用的诊断元数据基类。
 */
sealed class DiagnosticData {
    /**
     * 诊断所属顶层对象名。
     */
    abstract val containingObjectName: String
    /**
     * 诊断工厂名称。
     */
    abstract val name: String
    /**
     * 诊断锚定的 PSI 元素类型。
     */
    abstract val psiType: KType
    /**
     * 诊断携带的渲染参数列表。
     */
    abstract val parameters: List<DiagnosticParameter>
    /**
     * 诊断源码定位策略。
     */
    abstract val positioningStrategy: PositioningStrategy
}

/**
 * 普通错误或警告诊断的生成元数据。
 */
data class RegularDiagnosticData(
    /**
     * 诊断所属顶层对象名。
     */
    override val containingObjectName: String,
    /**
     * 诊断严重级别。
     */
    val severity: Severity,
    /**
     * 诊断工厂名称。
     */
    override val name: String,
    /**
     * 诊断锚定的 PSI 元素类型。
     */
    override val psiType: KType,
    /**
     * 诊断携带的渲染参数列表。
     */
    override val parameters: List<DiagnosticParameter>,
    /**
     * 诊断源码定位策略。
     */
    override val positioningStrategy: PositioningStrategy,
    /**
     * 该诊断是否允许通过 suppress 机制屏蔽。
     */
    val isSuppressible: Boolean,
) : DiagnosticData()

/**
 * 与语言特性弃用阶段绑定的诊断元数据。
 */
data class DeprecationDiagnosticData(
    /**
     * 诊断所属顶层对象名。
     */
    override val containingObjectName: String,
    /**
     * 控制该弃用诊断升为错误的语言特性。
     */
    val featureForError: LanguageFeature,
    /**
     * 诊断工厂名称。
     */
    override val name: String,
    /**
     * 诊断锚定的 PSI 元素类型。
     */
    override val psiType: KType,
    /**
     * 诊断携带的渲染参数列表。
     */
    override val parameters: List<DiagnosticParameter>,
    /**
     * 诊断源码定位策略。
     */
    override val positioningStrategy: PositioningStrategy,
) : DiagnosticData()

/**
 * 单个诊断参数的名称和类型。
 */
data class DiagnosticParameter(
    /**
     * 参数在诊断工厂和渲染器中的名称。
     */
    val name: String,
    /**
     * 参数的 Kotlin 反射类型。
     */
    val type: KType
)

/**
 * 诊断源码范围定位策略枚举。
 */
enum class PositioningStrategy {
    DEFAULT,
    SYNTAX_ERROR,
    VAL_OR_VAR_NODE,
    SECONDARY_CONSTRUCTOR_DELEGATION_CALL,
    DECLARATION_NAME,
    DECLARATION_NAME_ONLY,
    DECLARATION_SIGNATURE,
    DECLARATION_SIGNATURE_OR_DEFAULT,
    VISIBILITY_MODIFIER,
    MODALITY_MODIFIER,
    OPERATOR,
    PARAMETER_DEFAULT_VALUE,
    PARAMETERS_WITH_DEFAULT_VALUE,
    PARAMETER_VARARG_MODIFIER,
    DECLARATION_RETURN_TYPE,
    OVERRIDE_MODIFIER,
    MUT_MODIFIER,
    THROW_KEYWORD,
    ARRAY_LITERAL_LEFT_BRACKET,
    DOT_BY_QUALIFIED,
    OPEN_MODIFIER,
    WHEN_EXPRESSION,
    IF_EXPRESSION,
    ELSE_ENTRY,
    VARIANCE_MODIFIER,
    LATEINIT_MODIFIER,
    INLINE_OR_VALUE_MODIFIER,
    INNER_MODIFIER,
    SUSPEND_MODIFIER,
    SELECTOR_BY_QUALIFIED,
    REFERENCE_BY_QUALIFIED,
    REFERENCED_NAME_BY_QUALIFIED,
    PRIVATE_MODIFIER,
    COMPANION_OBJECT,
    CONST_MODIFIER,
    ARRAY_ACCESS,
    SAFE_ACCESS,
    AS_TYPE,
    USELESS_ELVIS,
    USELESS_ELVIS_LEFT,
    NAME_OF_NAMED_ARGUMENT,
    VALUE_ARGUMENTS,
    VALUE_ARGUMENTS_LIST,
    SUPERTYPES_LIST,
    RETURN_WITH_LABEL,
    VARIABLE_INITIALIZER,
    PATTERN_VARIABLE_INITIALIZER,

    WHOLE_ELEMENT,
    LONG_LITERAL_SUFFIX,
    REIFIED_MODIFIER,
    TYPE_PARAMETERS_LIST,
    FUNCTION_TYPE_RECEIVER,
    FUN_MODIFIER,
    FUN_INTERFACE,
    NAME_IDENTIFIER,
    QUESTION_MARK_BY_TYPE,
    ANNOTATION_USE_SITE,
    IMPORT_LAST_NAME,
    IMPORT_LAST_BUT_ONE_NAME,
    DATA_MODIFIER,
    SPREAD_OPERATOR,
    DECLARATION_WITH_BODY,
    NOT_SUPPORTED_IN_INLINE_MOST_RELEVANT,
    ACTUAL_DECLARATION_NAME,
    UNREACHABLE_CODE,
    CONTEXT_KEYWORD,
    INLINE_PARAMETER_MODIFIER,
    ABSTRACT_MODIFIER,
    LABEL,
    COMMAS,
    OPERATOR_MODIFIER,
    INFIX_MODIFIER,
    NON_FINAL_MODIFIER_OR_NAME,
    ENUM_MODIFIER,
    FIELD_KEYWORD,
    TAILREC_MODIFIER,
    EXTERNAL_MODIFIER,
    PROPERTY_DELEGATE,
    IMPORT_ALIAS,
    DECLARATION_START_TO_NAME,
    REDUNDANT_NULLABLE,
    INLINE_FUN_MODIFIER,
    CALL_ELEMENT_WITH_DOT,
    EXPECT_ACTUAL_MODIFIER,
    TYPEALIAS_TYPE_REFERENCE,
    SUPERTYPE_INITIALIZED_IN_EXPECTED_CLASS_DIAGNOSTIC,
    TYPE_ARGUMENT_LIST_OR_SELF,
    TYPE_ARGUMENT_LIST_OR_WITHOUT_RECEIVER,
    WHEN_GUARD,
    PACKAGE_DIRECTIVE_NAME_EXPRESSION,
    CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS,
    PROPERTY_DELEGATE_BY_KEYWORD,
    OUTERMOST_PARENTHESES_IN_ASSIGNMENT_LHS,
    DEPRECATION,
    ;

    /**
     * 生成错误列表时引用对应定位策略的表达式。
     */
    val expressionToCreate get() = "SourceElementPositioningStrategies.$name"

    /**
     * 定位策略枚举的生成辅助常量。
     */
    companion object {
        /**
         * 生成错误列表时需要导入的定位策略对象全限定名。
         */
        const val importToAdd = "org.cangnova.cangjie.cfir.diagnostics.SourceElementPositioningStrategies"
    }
}


