package org.cangnova.cangjie.cfir.resolve.dfa.cfg

import org.cangnova.cangjie.cfir.expressions.CfirBinaryExpression
import org.cangnova.cangjie.cfir.expressions.CfirBinaryOp
import org.cangnova.cangjie.cfir.expressions.CfirBinaryOpKind
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.source.CfirSourceOperator

/** raw 与完成源树使用同一短路 CFG；这里只读取真实运算身份与操作数，不转换 AST。 */
val CfirExpression.booleanOperatorKind: CfirBinaryOpKind
    get() = when (this) {
        is CfirBinaryOp -> kind.also { check(it == CfirBinaryOpKind.AND || it == CfirBinaryOpKind.OR) }
        is CfirBinaryExpression -> when (operation) {
            CfirSourceOperator.LOGICAL_AND -> CfirBinaryOpKind.AND
            CfirSourceOperator.LOGICAL_OR -> CfirBinaryOpKind.OR
            else -> error("Expression is not a Boolean short-circuit operator")
        }
        else -> error("Expression has no Boolean operator protocol")
    }

val CfirExpression.booleanOperatorLeft: CfirExpression
    get() = when (this) {
        is CfirBinaryOp -> left
        is CfirBinaryExpression -> left
        else -> error("Expression has no Boolean operands")
    }

val CfirExpression.booleanOperatorRight: CfirExpression
    get() = when (this) {
        is CfirBinaryOp -> right
        is CfirBinaryExpression -> right
        else -> error("Expression has no Boolean operands")
    }
