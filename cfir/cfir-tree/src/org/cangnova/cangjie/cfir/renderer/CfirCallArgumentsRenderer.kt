package org.cangnova.cangjie.cfir.renderer

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.name.Name

/**
 * 调用实参渲染器。
 *
 * 负责把普通实参列表、已解析实参到形参映射、注解实参和错误恢复参数统一渲染成调试文本。
 */
open class CfirCallArgumentsRenderer {
    /**
     * 当前 renderer 共享组件。
     */
    internal lateinit var components: CfirRendererComponents

    /**
     * 当前渲染 visitor。
     */
    protected val visitor: CfirRenderer.Visitor get() = components.visitor

    /**
     * 当前输出 printer。
     */
    protected val printer: CfirPrinter get() = components.printer

    /**
     * 表达式内联渲染器。
     */
    private val inlineExpressionRenderer: CfirInlineExpressionRenderer? get() = components.inlineExpressionRenderer

    /**
     * 渲染已解析实参到形参的映射。
     */
    open fun renderArgumentMapping(argumentMapping: LinkedHashMap<CfirExpression, CfirValueParameter>) {
        printer.print("(")
        argumentMapping.renderResolvedSeparated()
        printer.print(")")
    }

    /**
     * 渲染普通实参表达式列表。
     */
    open fun renderArguments(arguments: List<CfirExpression>) {
        printer.print("(")
        printer.print(arguments.joinToString { renderElementInline(it) })
        printer.print(")")
    }

    /**
     * 根据参数列表类型渲染普通参数或已解析参数映射。
     */
    open fun renderArguments(argumentList: CfirArgumentList) {
        when (argumentList) {
            is CfirResolvedArgumentList -> renderArgumentMapping(argumentList.mapping)
            else -> renderArguments(argumentList.arguments)
        }
    }

    /**
     * 渲染通用 CFIR 元素形式的参数列表。
     */
    open fun renderArgumentElements(arguments: List<CfirElement>) {
        printer.print("(")
        printer.print(arguments.joinToString { renderElementInline(it) })
        printer.print(")")
    }



    /**
     * 渲染命名参数映射。
     */
    private fun Map<Name, CfirElement>.renderSeparated() {
        for ((index, element) in this.entries.withIndex()) {
            val (name, argument) = element
            if (index > 0) {
                printer.print(", ")
            }
            printer.print("$name = ")
            argument.accept(visitor)
        }
    }

    /**
     * 渲染已解析实参映射。
     */
    private fun Map<CfirExpression, CfirValueParameter>.renderResolvedSeparated() {
        for ((index, element) in this.entries.withIndex()) {
            val (expression, parameter) = element
            if (index > 0) {
                printer.print(", ")
            }
            printer.print("${parameter.name} = ")
            printer.print(renderElementInline(expression))
        }
    }

    /**
     * 渲染已解析实参映射并附带常量求值结果。
     */
    private fun Map<CfirExpression, CfirValueParameter>.renderSeparatedWithEvaluatedValue(evaluated: Map<Name, CfirExpression>) {
        for ((index, element) in this.entries.withIndex()) {
            val (expression, parameter) = element
            val name = parameter.name
            if (index > 0) {
                printer.print(", ")
            }
            printer.print("$name = ")
            expression.accept(visitor)
            if (evaluated.containsKey(name)) {
                printer.print(" [evaluated = ")
                evaluated[name]?.accept(visitor)
                printer.print("]")
            }
        }
    }

    /**
     * 将单个元素渲染成一行表达式文本。
     */
    private fun renderElementInline(element: CfirElement): String {
        val expression = element as? CfirExpression ?: return "<expr>"
        return inlineExpressionRenderer?.render(expression) ?: "<expr>"
    }
}
