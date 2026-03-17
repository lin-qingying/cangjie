package org.cangnova.cangjie.cfir.expressions

/**
 * 比较操作符（CfirComparisonOp）→ 函数名映射。
 *
 * 用于将 AST 比较操作符转换为对应的函数名，便于通过内建操作符解析器查询。
 */
fun CfirComparisonOp.toFunctionName(): String = when (this) {
    CfirComparisonOp.EQ -> "equal"
    CfirComparisonOp.NE -> "notEqual"
    CfirComparisonOp.LT -> "less"
    CfirComparisonOp.GT -> "greater"
    CfirComparisonOp.LE -> "lessEqual"
    CfirComparisonOp.GE -> "greaterEqual"
}
