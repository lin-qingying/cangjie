package org.cangnova.cangjie.cfir.diagnostics

import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.annotations.BuiltInAnnotationKind
import org.cangnova.cangjie.name.Name

/** CJMP 诊断参数携带的语义事实；可读文案由诊断 renderer 统一生成。 */
sealed interface CfirCjmpDiagnosticArgument {
    /** 用声明种类作为诊断参数。 */
    data class DeclarationKind(val kind: CfirCjmpDiagnosticLabel) : CfirCjmpDiagnosticArgument

    /** 用声明名作为诊断参数。 */
    data class DeclarationName(val declaration: CfirDeclaration) : CfirCjmpDiagnosticArgument

    /** 用具体变量的可变性作为诊断参数。 */
    data class VariableKind(val isVar: Boolean) : CfirCjmpDiagnosticArgument

    /** 用 common/specific 角色作为诊断参数。 */
    data class Side(val side: CfirCjmpSide) : CfirCjmpDiagnosticArgument

    /** 用确定存在的声明名称作为诊断参数。 */
    data class Name(val name: org.cangnova.cangjie.name.Name) : CfirCjmpDiagnosticArgument

    /** 缺少 body 的特定成员可能没有可显示的容器名称；null 保留该可选语义。 */
    data class ContainerName(val name: org.cangnova.cangjie.name.Name?) : CfirCjmpDiagnosticArgument

    /** 由 renderer 根据声明及其外层声明生成 NOT_MATCHED 文案。 */
    data class NotMatchedDeclaration(
        val declaration: CfirDeclaration,
        val outerDeclaration: CfirDeclaration?,
    ) : CfirCjmpDiagnosticArgument

    /** 按实现种类的结构化类别生成 MULTIPLE_COMMON_IMPLEMENTATIONS 文案。 */
    data class ImplementationKind(val kind: CfirCjmpImplementationKind) : CfirCjmpDiagnosticArgument

    /** 按扩展声明的源码类型或语义类型生成诊断文案。 */
    data class ExtendedType(val declaration: CfirExtend) : CfirCjmpDiagnosticArgument

    /** 使用明确列举的诊断标签，不在 checker 中拼接消息文本。 */
    data class Label(val label: CfirCjmpDiagnosticLabel) : CfirCjmpDiagnosticArgument
}

/**
 * CJMP related-information 的语义载荷。
 *
 * checker 只选择对应的诊断事实；英文提示和注解表达式格式由 diagnostic-renderers 统一呈现。
 */
sealed interface CfirCjmpRelatedInformationMessage {
    /** specific 声明会继承对应 common 声明的弃用状态。 */
    data object DeprecatedAnnotationInheritedFromCommon : CfirCjmpRelatedInformationMessage

    /** 普通注解没有与另一侧相等的身份及实参。 */
    data class MismatchedAnnotation(val annotation: CfirCjmpAnnotationPresentation) : CfirCjmpRelatedInformationMessage

    /** 官方 declaration attribute 不一致；CJO 可能没有可恢复的注解表达式节点。 */
    data class MismatchedAnnotationAttribute(
        val kind: BuiltInAnnotationKind,
        val annotation: CfirCjmpAnnotationPresentation?,
    ) : CfirCjmpRelatedInformationMessage

    /** 注解只出现在 specific 侧时，关联到对应的 common 声明。 */
    data object CorrespondingCommonDeclaration : CfirCjmpRelatedInformationMessage

    /** 注解只出现在 specific 侧时给出的修正建议。 */
    data object AnnotationCorrectionAdvice : CfirCjmpRelatedInformationMessage
}

/** 注解源码展示名与表达式节点；不提前格式化参数或诊断消息。 */
data class CfirCjmpAnnotationPresentation(
    val annotation: CfirAnnotation,
    val sourceName: String,
)

/** CJMP 诊断中有正式语义的声明种类标签。 */
enum class CfirCjmpDiagnosticLabel {
    CLASS,
    STRUCT,
    INTERFACE,
    ENUM,
    EXTEND,
    PROPERTY,
    FUNCTION,
    VARIABLE,
    TYPE_ALIAS,
    FIELD,
    LET,
    VAR,
    OPEN_ABSTRACT,
}

/** common/specific 参数的角色。 */
enum class CfirCjmpSide {
    COMMON,
    SPECIFIC,
}

/** 诊断所区分的实现种类；值相等即代表同一类官方实现分组。 */
sealed interface CfirCjmpImplementationKind {
    /** 按声明种类分组的实现。 */
    data class Declaration(val kind: CfirCjmpDiagnosticLabel) : CfirCjmpImplementationKind

    /** 属性实现按属性名分别报告。 */
    data class Property(val name: Name) : CfirCjmpImplementationKind

    /** 枚举构造器实现按枚举名分别报告。 */
    data class EnumConstructor(val enumName: Name) : CfirCjmpImplementationKind
}
