package org.cangnova.cangjie.cfir.tree.generator.printer

import org.cangnova.cangjie.cfir.tree.generator.CfirTree
import org.cangnova.cangjie.cfir.tree.generator.cfirTransformerType
import org.cangnova.cangjie.cfir.tree.generator.cfirVisitorType
import org.cangnova.cangjie.cfir.tree.generator.cfirVisitorVoidType
import org.cangnova.cangjie.cfir.tree.generator.model.Element
import org.cangnova.cangjie.cfir.tree.generator.model.Field
import org.cangnova.cangjie.generators.tree.AbstractElementPrinter
import org.cangnova.cangjie.generators.tree.AbstractFieldPrinter
import org.cangnova.cangjie.generators.tree.TypeVariable
import org.cangnova.cangjie.generators.tree.printer.ImportCollectingPrinter
import org.cangnova.cangjie.generators.tree.printer.printAcceptChildrenMethod
import org.cangnova.cangjie.generators.tree.printer.printAcceptChildrenVoidMethod
import org.cangnova.cangjie.generators.tree.printer.printAcceptVoidMethod
import org.cangnova.cangjie.generators.tree.printer.printAnnotation
import org.cangnova.cangjie.generators.tree.printer.printTransformChildrenMethod
import org.cangnova.cangjie.generators.tree.printer.printTransformMethod
import org.cangnova.cangjie.generators.tree.printer.printAcceptMethod

internal class ElementPrinter(printer: ImportCollectingPrinter) : AbstractElementPrinter<Element, Field>(printer) {
    override fun makeFieldPrinter(printer: ImportCollectingPrinter): AbstractFieldPrinter<Field> =
        object : AbstractFieldPrinter<Field>(printer) {
            override fun forceMutable(field: Field): Boolean = field.isMutable
        }

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

        val replaceableFields = element.allFields.filter { it.withReplace }
        val abstractFunPrefix = if (element.kind?.isInterface == true) {
            "fun"
        } else {
            "abstract fun"
        }
        for (field in replaceableFields) {
            println()
            field.replaceOptInAnnotation?.let { println("@OptIn(${it.render()}::class)") }
            field.deprecation?.let { printAnnotation(it) }
            val prefix = if (field.isOverride) "override $abstractFunPrefix" else abstractFunPrefix
            println("$prefix replace${field.name.replaceFirstChar(Char::uppercaseChar)}(new${field.name.replaceFirstChar(Char::uppercaseChar)}: ${field.typeRef.render()})")
            println()
        }

        val transformableFields = element.allFields.filter { it.withTransform }
        for (field in transformableFields) {
            println()
            field.deprecation?.let { printAnnotation(it) }
            val prefix = if (field.isOverride) "override $abstractFunPrefix" else abstractFunPrefix
            println("$prefix <D> transform${field.name.replaceFirstChar(Char::uppercaseChar)}(transformer: ${cfirTransformerType.render()}<D>, data: D): ${element.typeName}")
            println()
        }

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
