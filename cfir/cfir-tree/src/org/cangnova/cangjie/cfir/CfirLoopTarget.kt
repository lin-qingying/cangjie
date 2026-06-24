package org.cangnova.cangjie.cfir

import org.cangnova.cangjie.cfir.expressions.CfirLoopExpression

/**
 * 循环 jump 的内部目标。
 *
 * 现阶段仓颉只实现“隐式最近循环”绑定，因此 `labelName` 默认恒为 `null`。
 *
 * @param labelName 循环 label 名称；当前通常为 `null`。
 */
class CfirLoopTarget(
    labelName: String? = null,
) : CfirAbstractTarget<CfirLoopExpression>(labelName) {
    /**
     * 绑定到该 target 的循环表达式。
     */
    override lateinit var _labeledElement: CfirLoopExpression
}
