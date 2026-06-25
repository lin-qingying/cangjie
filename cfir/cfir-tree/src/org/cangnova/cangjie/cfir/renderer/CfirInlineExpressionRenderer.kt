package org.cangnova.cangjie.cfir.renderer

import org.cangnova.cangjie.cfir.render.ConeTypeRenderer
import org.cangnova.cangjie.cfir.render.ConeTypeRendererForDebugging
import org.cangnova.cangjie.cfir.expressions.*

/**
 * 单行表达式渲染器。
 *
 * 该渲染器用于参数、属性值和模式中的紧凑调试输出，不承担完整 CFIR 树 pretty print。
 *
 * @property referenceRenderer 引用渲染器。
 * @property typeRenderer 类型渲染器。
 */
open class CfirInlineExpressionRenderer(
    /**
     * 用于渲染 callee reference 与符号引用的组件。
     */
    protected val referenceRenderer: CfirReferenceRenderer,
    /**
     * 用于在单行表达式中渲染类型引用的 cone 类型渲染器。
     */
    protected val typeRenderer: ConeTypeRenderer = ConeTypeRendererForDebugging(),
) {
    /**
     * 将表达式渲染为单行文本。
     */
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
