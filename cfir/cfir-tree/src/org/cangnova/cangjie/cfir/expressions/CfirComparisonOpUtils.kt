package org.cangnova.cangjie.cfir.expressions

import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions

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

/**
 * 将比较操作符转换为内建 operator 名称。
 *
 * 该名称用于按运算符约定查询 callable，而不是直接使用源码中的符号文本。
 */
fun CfirComparisonOp.toOperatorName(): Name = when (this) {
    CfirComparisonOp.EQ -> OperatorNameConventions.EQUALS
    CfirComparisonOp.NE -> OperatorNameConventions.NOT_EQUALS
    CfirComparisonOp.LT -> OperatorNameConventions.COMPARE_LT
    CfirComparisonOp.GT -> OperatorNameConventions.COMPARE_GT
    CfirComparisonOp.LE -> OperatorNameConventions.COMPARE_LTEQ
    CfirComparisonOp.GE -> OperatorNameConventions.COMPARE_GTEQ
}
