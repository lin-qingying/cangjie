package org.cangnova.cangjie.cfir.renderer

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.name.Name

open class CfirCallArgumentsRenderer {
    internal lateinit var components: CfirRendererComponents
    protected val visitor: CfirRenderer.Visitor get() = components.visitor
    protected val printer: CfirPrinter get() = components.printer



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
}
