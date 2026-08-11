package org.cangnova.cangjie.cfir.expressions

import org.cangnova.cangjie.cfir.types.ConeCangJieType

/**
 * 普通赋值在 RHS 目标类型检查完成后的结构化类型不匹配结果。
 *
 * 该结果只属于当前赋值节点，不改变 RHS 在 initializer、return、调用实参或 Join 中共享的
 * 全局表达式类型。resolve 阶段负责写入类型关系和根有效性；checker 只按该结果渲染基础诊断，
 * 并把赋值节点保留的 RHS 语法交给共享分类器选择官方规定的专用诊断。
 *
 * @property expectedType 左值要求的目标类型。
 * @property actualType RHS 根节点失效前捕获的实际类型。
 * @property primaryDiagnostic RHS expected-type 检查产生的基础诊断。
 * @property rhsRootValidity RHS 检查失败后根节点类型是否仍然有效。
 */
data class CfirAssignmentTypeMismatchOutcome(
    val expectedType: ConeCangJieType,
    val actualType: ConeCangJieType,
    val primaryDiagnostic: CfirAssignmentTypeMismatchPrimaryDiagnostic,
    val rhsRootValidity: CfirAssignmentRhsRootValidity,
)

/**
 * RHS 目标类型检查失败后，表达式根类型的官方有效性状态。
 */
enum class CfirAssignmentRhsRootValidity {
    /** RHS 根类型仍有效，赋值层需要追加 `TYPE_INCOMPATIBLE`。 */
    VALID_AFTER_MISMATCH,

    /** RHS 根类型已经失效，赋值层不得追加派生 wrapper。 */
    INVALID_AFTER_MISMATCH,
}

/**
 * RHS expected-type 检查产生的结构化基础诊断。
 *
 * 该结果由真实的 RHS 类型检查 owner 写入 frame；assignment checker 不再根据表达式
 * 语法或接收者重新推导类型关系与根有效性。
 */
sealed interface CfirAssignmentTypeMismatchPrimaryDiagnostic {
    /** 使用通用 `TYPE_MISMATCH`。 */
    data object TypeMismatch : CfirAssignmentTypeMismatchPrimaryDiagnostic

    /** 直接字面量不能转换为 RHS expected type。 */
    data class CannotConvertLiteral(
        /** 官方诊断渲染所需的字面量类别文本。 */
        val literalDescription: String,
    ) : CfirAssignmentTypeMismatchPrimaryDiagnostic
}
