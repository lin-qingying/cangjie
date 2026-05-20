package org.cangnova.cangjie.cfir.renderer

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.name.Name

open class CfirCallArgumentsRenderer {
    internal lateinit var components: CfirRendererComponents
    protected val visitor: CfirRenderer.Visitor get() = components.visitor
    protected val printer: CfirPrinter get() = components.printer
    private val inlineExpressionRenderer: CfirInlineExpressionRenderer? get() = components.inlineExpressionRenderer

    open fun renderArgumentMapping(argumentMapping: LinkedHashMap<CfirExpression, CfirValueParameter>) {
        printer.print("(")
        argumentMapping.renderResolvedSeparated()
        printer.print(")")
    }

    open fun renderArguments(arguments: List<CfirExpression>) {
        printer.print("(")
        printer.print(arguments.joinToString { renderElementInline(it) })
        printer.print(")")
    }

    open fun renderArguments(argumentList: CfirArgumentList) {
        when (argumentList) {
            is CfirResolvedArgumentList -> renderArgumentMapping(argumentList.mapping)
            else -> renderArguments(argumentList.arguments)
        }
    }

    open fun renderArgumentElements(arguments: List<CfirElement>) {
        printer.print("(")
        printer.print(arguments.joinToString { renderElementInline(it) })
        printer.print(")")
    }



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

    private fun renderElementInline(element: CfirElement): String {
        val expression = element as? CfirExpression ?: return "<expr>"
        return inlineExpressionRenderer?.render(expression) ?: "<expr>"
    }
}
