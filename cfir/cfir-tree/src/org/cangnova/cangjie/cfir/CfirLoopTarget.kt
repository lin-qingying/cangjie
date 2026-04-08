package org.cangnova.cangjie.cfir

import org.cangnova.cangjie.cfir.expressions.CfirLoopExpression

/**
 * 循环 jump 的内部目标。
 *
 * 现阶段仓颉只实现“隐式最近循环”绑定，因此 `labelName` 默认恒为 `null`。
 */
class CfirLoopTarget(
    labelName: String? = null,
) : CfirAbstractTarget<CfirLoopExpression>(labelName) {
    override lateinit var _labeledElement: CfirLoopExpression
}
