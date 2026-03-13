package org.cangjie.cfir.tree.generator.printer

import org.cangjie.cfir.tree.generator.cfirImplementationDetailType
import org.cangjie.cfir.tree.generator.cfirTransformerType
import org.cangjie.cfir.tree.generator.cfirVisitorType
import org.cangjie.cfir.tree.generator.model.Element
import org.cangjie.cfir.tree.generator.model.Field
import org.cangjie.cfir.tree.generator.model.Implementation
import org.cangjie.cfir.tree.generator.model.ListField
import org.cangjie.cfir.tree.generator.model.SimpleField
import org.cangjie.cfir.tree.generator.pureAbstractElementType
import org.cangjie.generators.tree.AbstractFieldPrinter
import org.cangjie.generators.tree.AbstractImplementationPrinter
import org.cangjie.generators.tree.ClassRef
import org.cangjie.generators.tree.ImplementationKind
import org.cangjie.generators.tree.TypeVariable
import org.cangjie.generators.tree.printer.call
import org.cangjie.generators.tree.printer.ImportCollectingPrinter
import org.cangjie.generators.tree.printer.printAcceptChildrenMethod
import org.cangjie.generators.tree.printer.printTransformChildrenMethod
import org.cangjie.generators.util.printBlock

internal class ImplementationPrinter(printer: ImportCollectingPrinter) :
    AbstractImplementationPrinter<Implementation, Element, Field>(printer) {
    override val implementationOptInAnnotation: ClassRef<*>
        get() = cfirImplementationDetailType

    override fun getPureAbstractElementType(implementation: Implementation): ClassRef<*> =
        pureAbstractElementType

    override fun makeFieldPrinter(printer: ImportCollectingPrinter): AbstractFieldPrinter<Field> =
        object : AbstractFieldPrinter<Field>(printer) {}

    override fun ImportCollectingPrinter.printAdditionalMethods(implementation: Implementation) {
        val kind = implementation.kind ?: error("Expected non-null implementation kind")
        val isInterface = kind == ImplementationKind.Interface || kind == ImplementationKind.SealedInterface
        val isAbstract = kind == ImplementationKind.AbstractClass || kind == ImplementationKind.SealedClass

        if (implementation.hasAcceptChildrenMethod) {
            printAcceptChildrenMethod(implementation, cfirVisitorType, TypeVariable("R"), override = true)
            if (isInterface || isAbstract) {
                println()
            } else {
                printBlock {
                    for (field in implementation.walkableChildren) {
                        when (field) {
                            is SimpleField -> println("${field.name}${field.call()}accept(visitor, data)")
                            is ListField -> println("${field.name}${field.call()}forEach { it.accept(visitor, data) }")
                            else -> {}
                        }
                    }
                }
            }
        }

        if (implementation.hasTransformChildrenMethod) {
            printTransformChildrenMethod(implementation, cfirTransformerType, implementation, override = true)
            if (isInterface || isAbstract) {
                println()
            } else {
                printBlock {
                    for (field in implementation.transformableChildren) {
                        when (field) {
                            is SimpleField -> println("${field.name}${field.call()}transform<org.cangjie.cfir.CfirElement, D>(transformer, data)")
                            is ListField -> println("${field.name}${field.call()}forEach { it.transform<org.cangjie.cfir.CfirElement, D>(transformer, data) }")
                            else -> {}
                        }
                    }
                    println("return this")
                }
            }
        }
    }
}
