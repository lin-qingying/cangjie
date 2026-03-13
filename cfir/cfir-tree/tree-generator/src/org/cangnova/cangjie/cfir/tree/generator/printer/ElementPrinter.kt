package org.cangjie.cfir.tree.generator.printer

import org.cangjie.cfir.tree.generator.CfirTree
import org.cangjie.cfir.tree.generator.cfirTransformerType
import org.cangjie.cfir.tree.generator.cfirVisitorType
import org.cangjie.cfir.tree.generator.cfirVisitorVoidType
import org.cangjie.cfir.tree.generator.model.Element
import org.cangjie.cfir.tree.generator.model.Field
import org.cangjie.generators.tree.AbstractElementPrinter
import org.cangjie.generators.tree.AbstractFieldPrinter
import org.cangjie.generators.tree.TypeVariable
import org.cangjie.generators.tree.printer.ImportCollectingPrinter
import org.cangjie.generators.tree.printer.printAcceptChildrenMethod
import org.cangjie.generators.tree.printer.printAcceptChildrenVoidMethod
import org.cangjie.generators.tree.printer.printAcceptVoidMethod
import org.cangjie.generators.tree.printer.printTransformChildrenMethod
import org.cangjie.generators.tree.printer.printTransformMethod
import org.cangjie.generators.tree.printer.printAcceptMethod

internal class ElementPrinter(printer: ImportCollectingPrinter) : AbstractElementPrinter<Element, Field>(printer) {
    override fun makeFieldPrinter(printer: ImportCollectingPrinter): AbstractFieldPrinter<Field> =
        object : AbstractFieldPrinter<Field>(printer) {}

    override fun ImportCollectingPrinter.printAdditionalMethods(element: Element) {
        val treeName = "CFIR"
        printAcceptMethod(element, cfirVisitorType, hasImplementation = true, treeName = treeName)
        printTransformMethod(
            element = element,
            transformerClass = cfirTransformerType,
            implementation = "transformer.transform${element.name}(this, data)",
            returnType = TypeVariable("E", listOf(CfirTree.rootElement)),
            treeName = treeName,
        )

            if (element.isRootElement) {
                println()
                printAcceptVoidMethod(cfirVisitorVoidType, treeName)
                printAcceptChildrenMethod(
                    element = element,
                    visitorClass = cfirVisitorType,
                    visitorResultType = TypeVariable("R"),
                )
                println()
                printAcceptChildrenVoidMethod(cfirVisitorVoidType)
                println()
                printTransformChildrenMethod(
                    element = element,
                    transformerClass = cfirTransformerType,
                    returnType = CfirTree.rootElement,
                )
            println()
        }
    }
}
