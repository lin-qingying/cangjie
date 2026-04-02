package org.cangnova.cangjie.cfir.tree.generator.printer

import org.cangnova.cangjie.cfir.tree.generator.CfirTree
import org.cangnova.cangjie.cfir.tree.generator.cfirImplementationDetailType
import org.cangnova.cangjie.cfir.tree.generator.cfirRendererType
import org.cangnova.cangjie.cfir.tree.generator.cfirTransformerType
import org.cangnova.cangjie.cfir.tree.generator.cfirVisitorType
import org.cangnova.cangjie.cfir.tree.generator.cfirVisitorVoidType
import org.cangnova.cangjie.cfir.tree.generator.model.Element
import org.cangnova.cangjie.cfir.tree.generator.model.Field
import org.cangnova.cangjie.cfir.tree.generator.util.replaceFunctionDeclaration
import org.cangnova.cangjie.generators.tree.AbstractElementPrinter
import org.cangnova.cangjie.generators.tree.AbstractFieldPrinter
import org.cangnova.cangjie.generators.tree.TypeRefWithNullability
import org.cangnova.cangjie.generators.tree.TypeVariable
import org.cangnova.cangjie.generators.tree.printer.ImportCollectingPrinter
import org.cangnova.cangjie.generators.tree.printer.printAcceptChildrenMethod
import org.cangnova.cangjie.generators.tree.printer.printAcceptChildrenVoidMethod
import org.cangnova.cangjie.generators.tree.printer.printAcceptMethod
import org.cangnova.cangjie.generators.tree.printer.printAcceptVoidMethod
import org.cangnova.cangjie.generators.tree.printer.printTransformChildrenMethod
import org.cangnova.cangjie.generators.tree.printer.printTransformMethod

private val elementsWithReplaceSource = setOf(
    CfirTree.qualifiedAccessExpression,
)

internal class ElementPrinter(printer: ImportCollectingPrinter) : AbstractElementPrinter<Element, Field>(printer) {
    override fun makeFieldPrinter(printer: ImportCollectingPrinter): AbstractFieldPrinter<Field> =
        object : AbstractFieldPrinter<Field>(printer) {}

    override fun ImportCollectingPrinter.printAdditionalMethods(element: Element) {
        val kind = element.kind ?: error("Expected non-null element kind")
        with(element) {
            val treeName = "CFIR"
            printAcceptMethod(element, cfirVisitorType, hasImplementation = true, treeName = treeName)
            printTransformMethod(
                element = element,
                transformerClass = cfirTransformerType,
                implementation = "transformer.transform${element.name}(this, data)",
                returnType = TypeVariable("E", listOf(CfirTree.rootElement)),
                treeName = treeName,
            )

            fun Field.replaceDeclaration(
                override: Boolean,
                overriddenType: TypeRefWithNullability? = null,
                forceNullable: Boolean = false,
            ) {
                println()
                if (name == "source") {
                    println("@", cfirImplementationDetailType.render())
                }
                replaceFunctionDeclaration(this, override, kind, overriddenType, forceNullable)
                println()
            }

            allFields.filter { it.withReplace }.forEach { field ->
                val clazz = field.typeRef.copy(nullable = false)
                val clazzRender = clazz.render()
                val overriddenClasses = field.overriddenFields
                    .map { it.typeRef.copy(nullable = false) }
                    .distinctBy { it.render() }

                val override = overriddenClasses.any { it.render() == clazzRender } && !(field.name == "source" && element in elementsWithReplaceSource)
                field.replaceDeclaration(override, forceNullable = field.receiveNullableTypeInReplace)

                for (overriddenClass in overriddenClasses.filter { it.render() != clazzRender }) {
                    field.replaceDeclaration(true, overriddenType = overriddenClass)
                }
            }

            for (field in allFields) {
                if (!field.withTransform) continue
                println()
                transformFunctionDeclaration(
                    field = field,
                    returnType = element.withSelfArgs(),
                    override = field.overriddenFields.any { it.withTransform },
                    implementationKind = kind,
                )
                println()
            }

            if (needTransformOtherChildren) {
                println()
                transformOtherChildrenFunctionDeclaration(
                    element.withSelfArgs(),
                    override = element.elementParents.any { it.element.needTransformOtherChildren },
                    kind,
                )
                println()
            }

            if (isRootElement) {
                println()
                printAcceptVoidMethod(cfirVisitorVoidType, treeName)
                printAcceptChildrenMethod(
                    element = element,
                    visitorClass = cfirVisitorType,
                    visitorResultType = TypeVariable("R"),
                )
                println()
                println()
                printAcceptChildrenVoidMethod(cfirVisitorVoidType)
                printTransformChildrenMethod(
                    element = element,
                    transformerClass = cfirTransformerType,
                    returnType = CfirTree.rootElement,
                )
                println()
            }
        }

        if (element == CfirTree.declaration) {
            println()
            println("override fun toString(): String = ${cfirRendererType.render()}.withReadability().renderElementAsString(this)")
            println()
        }
    }
}
