package org.cangnova.cangjie.analysis.api.components

import com.intellij.psi.PsiElement
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
     * 该 PSI 元素在当前位置的期望类型；若上下文不施加类型约束则返回 `null`。
     *
     * 该语义与表达式实际类型不同：
     * - `expressionType` 表示解析后的真实结果类型；
     * - `expectedType` 表示当前位置上下文要求它满足的目标类型。
     *
     * 覆盖的上下文（按约束来源是否确定排列，实现须与之一致）：
     * 1. **调用实参位**——位置落在实参表达式内时，取被调方该位形参的类型；
     *    位置实参按位次、命名实参（`name: value`）按形参名绑定，两种写法的结论相同；
     * 2. `return` 表达式、显式类型的初始化器与赋值右值、命名参数默认值；
     * 3. 条件与分支结果等由上下文决定的表达式；
     * 4. 不完整调用：仅当本次解析保留的候选**一致**时给出确定类型。
     *
     * 约束不确定时必须返回 `null`，不得任取一个候选或兄弟表达式：
     * 调用方（补全/高亮）用 `null` 表示「未知」，而「未知」的行为与「已知但不匹配」不同——
     * 前者不参与过滤与加权，后者会。
     */
    val PsiElement.expectedType: CaType?

    /**
     * 可调用声明对外暴露的返回类型;不可推断时返回 `null`。
     */
    val CjCallableDeclaration.returnType: CaType?
}
