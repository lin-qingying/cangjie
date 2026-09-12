package org.cangnova.cangjie.cfir.expressions

/** 命名/inout 实参只包装语法；可选接收者与整条可选链会改变类型，不能透明转发目标或内部类型。 */
val CfirWrappedExpression.isTypeTransparent: Boolean
    get() = this !is CfirOptionalExpression && this !is CfirOptionalChainExpression

/**
 * 按求值顺序取得当前可选链接收者路径上的每个 `?`。
 *
 * 对位官方 GetOptionalExpr / DesugarOptionalChainWithMatchCase：只沿成员、调用、
 * 下标和修改目标的接收者路径前进。实参和独立的嵌套可选链拥有自己的检查边界。
 */
fun CfirExpression.optionalChainSubjects(): List<CfirOptionalExpression> {
    val subjects = mutableListOf<CfirOptionalExpression>()
    var current: CfirExpression? = this
    while (current != null) {
        current = when (val expression = current) {
            is CfirOptionalExpression -> {
                subjects += expression
                expression.expression
            }
            is CfirOptionalChainExpression -> null
            is CfirQualifiedAccessExpression -> expression.explicitReceiver ?: expression.dispatchReceiver
            is CfirSubscriptExpression -> expression.receiver
            is CfirAssignment -> expression.lValue
            is CfirAugmentedAssignment -> expression.leftArgument
            is CfirIncrementDecrementExpression -> expression.expression
            is CfirWrappedExpression -> expression.expression
            else -> null
        }
    }
    return subjects.asReversed()
}
