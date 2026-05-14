package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjExpression

/**
 * 表达式与可调用声明的类型查询协议。
 *
 * 设计要点/职责:
 * - 暴露表达式的实际类型(反映类型推断、智能转换、隐式转换之后的稳定结果)。
 * - 暴露可调用声明的返回类型,作为 IDE 类型显示、签名、补全等流程的统一入口。
 * - 协议层结果允许为 `null`,以表示"未参与类型计算"或"不可推断"的语义,不抛异常。
 *
 * 对齐 Kotlin Analysis API 的 `KaExpressionTypeProvider`。
 */
interface CaExpressionTypeProvider : CaLifetimeOwner {
    /**
     * 该表达式的解析后类型;不参与类型计算或不在表达式树中的元素返回 `null`。
     */
    val CjExpression.expressionType: CaType?

    /**
     * 可调用声明对外暴露的返回类型;不可推断时返回 `null`。
     */
    val CjCallableDeclaration.returnType: CaType?
}
