package org.cangnova.cangjie.cfir.renderer

import org.cangnova.cangjie.cfir.render.ConeTypeRenderer
import org.cangnova.cangjie.cfir.render.ConeTypeRendererForDebugging
import org.cangnova.cangjie.cfir.expressions.*

open class CfirInlineExpressionRenderer(
    protected val referenceRenderer: CfirReferenceRenderer,
    protected val typeRenderer: ConeTypeRenderer = ConeTypeRendererForDebugging(),
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
        is CfirAnnotationCall -> "@${renderTypeRefForDebug(expression.typeRef, typeRenderer)}(${
            expression.argumentList.arguments.joinToString { render(it) }
        })"
        is CfirAnnotation -> "@${renderTypeRefForDebug(expression.typeRef, typeRenderer)}(${
            expression.arguments.joinToString { argument ->
                (argument as? CfirExpression)?.let(::render) ?: "<expr>"
            }
        })"
        else -> "<expr>"
    }
}
