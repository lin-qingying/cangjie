package org.cangnova.cangjie.cfir.renderer

import org.cangnova.cangjie.cfir.expressions.*

open class CfirInlineExpressionRenderer(
    protected val referenceRenderer: CfirReferenceRenderer,
) {
    open fun render(expression: CfirExpression): String = when (expression) {
        is CfirLiteralExpression -> when (expression.kind) {
            CfirLiteralKind.STRING -> "\"${expression.value}\""
            else -> "${expression.value}"
        }
        is CfirFunctionCall -> "${referenceRenderer.render(expression.calleeReference)}(${
            expression.argumentList.arguments.joinToString { render(it) }
        })"
        is CfirNamedAccessExpression -> referenceRenderer.render(expression.calleeReference)
        is CfirQualifiedAccessExpression -> referenceRenderer.render(expression.calleeReference)
        is CfirComparisonExpression -> "${render(expression.left)} ${expression.operation.symbol} ${render(expression.right)}"
        is CfirBinaryOp -> "${render(expression.left)} ${expression.kind.symbol} ${render(expression.right)}"
        else -> "<expr>"
    }
}