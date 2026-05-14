package org.cangnova.cangjie.cfir.renderer

import org.cangnova.cangjie.cfir.patterns.*
import org.cangnova.cangjie.cfir.render.ConeTypeRenderer

open class CfirPatternRenderer(
    protected val typeRenderer: ConeTypeRenderer,
    protected val referenceRenderer: CfirReferenceRenderer,
    protected val inlineExpressionRenderer: CfirInlineExpressionRenderer,
) {
    open fun render(pattern: CfirPattern): String = when (pattern) {
        is CfirExpressionPattern -> "expr(${inlineExpressionRenderer.render(pattern.expression)})"
        is CfirOrPattern -> pattern.alternatives.joinToString(" | ") { render(it) }
        is CfirWildcardPattern -> "_"
        is CfirConstPattern -> "const(${inlineExpressionRenderer.render(pattern.expression)})"
        is CfirVarOrEnumPattern -> "deferred(${pattern.name.asString()})"
        is CfirBindingPattern -> buildString {
            append(pattern.name.asString())
            pattern.typeRef?.let {
                val renderedTypeRef = renderTypeRefForDebug(it, typeRenderer)
                if (renderedTypeRef.isNotEmpty()) {
                    append(": $renderedTypeRef")
                }
            }
            pattern.nestedPattern?.let { append(" @ ${render(it)}") }
        }
        is CfirTuplePattern -> "(${pattern.elements.joinToString { render(it) }})"
        is CfirEnumPattern -> "${referenceRenderer.render(pattern.constructorReference)}(${
            pattern.arguments.joinToString { render(it) }
        })"
        is CfirTypePattern -> buildString {
            append("is ${renderTypeRefForDebug(pattern.typeRef, typeRenderer)}")
            pattern.bindingName?.let { append(" ${it.asString()}") }
        }
        else -> "<unknown-pattern>"
    }
}
