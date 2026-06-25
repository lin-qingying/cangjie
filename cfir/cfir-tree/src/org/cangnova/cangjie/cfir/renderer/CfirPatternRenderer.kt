package org.cangnova.cangjie.cfir.renderer

import org.cangnova.cangjie.cfir.patterns.*
import org.cangnova.cangjie.cfir.render.ConeTypeRenderer

/**
 * 模式节点单行渲染器。
 *
 * @property typeRenderer 类型渲染器。
 * @property referenceRenderer 引用渲染器。
 * @property inlineExpressionRenderer 表达式单行渲染器。
 */
open class CfirPatternRenderer(
    /**
     * 模式中类型部分使用的 cone 类型渲染器。
     */
    protected val typeRenderer: ConeTypeRenderer,
    /**
     * 模式中构造器或 callable 引用使用的引用渲染器。
     */
    protected val referenceRenderer: CfirReferenceRenderer,
    /**
     * 模式中常量表达式或表达式模式使用的单行表达式渲染器。
     */
    protected val inlineExpressionRenderer: CfirInlineExpressionRenderer,
) {
    /**
     * 将模式渲染为单行调试文本。
     */
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
    }
}
