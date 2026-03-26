package org.cangnova.cangjie.cfir.tree.generator

import org.cangnova.cangjie.cfir.tree.generator.printer.*
import org.cangnova.cangjie.generators.tree.detectBaseTransformerTypes
import org.cangnova.cangjie.generators.tree.InterfaceAndAbstractClassConfigurator
import org.cangnova.cangjie.generators.tree.printer.TreeGenerator
import java.io.File

fun main(args: Array<String>) {
    val generationPath = args.firstOrNull()?.let(::File) ?: File("../../gen").canonicalFile

    val model = CfirTree.build()
    TreeGenerator(generationPath, "cfir/cfir-tree/tree-generator/Readme.md").run {
        model.inheritFields()
        detectBaseTransformerTypes(model)

        ImplementationConfigurator.configureImplementations(model)
        val implementations = model.elements.flatMap { it.implementations }
        InterfaceAndAbstractClassConfigurator(model.elements + implementations).configureInterfacesAndAbstractClasses()
        model.addPureAbstractElement(pureAbstractElementType)

        val builderConfigurator = BuilderConfigurator(model)
        builderConfigurator.configureBuilders()
        model.specifyHasAcceptAndTransformChildrenMethods()

        printElements(model, ::ElementPrinter)
        printElementImplementations(
            implementations.filterNot { it.element == CfirTree.errorTypeRef },
            ::ImplementationPrinter
        )
        printElementBuilders(
            implementations
                .filterNot { it.element == CfirTree.errorTypeRef }
                .mapNotNull { it.builder } + builderConfigurator.intermediateBuilders,
            ::BuilderPrinter
        )
        printVisitors(
            model,
            listOf(
                cfirVisitorType to { printer, visitorType -> VisitorPrinter(printer, visitorType, false) },
                cfirDefaultVisitorType to { printer, visitorType -> VisitorPrinter(printer, visitorType, true) },
                cfirVisitorVoidType to { printer, visitorType -> VisitorVoidPrinter(printer, visitorType) },
                cfirDefaultVisitorVoidType to { printer, visitorType -> DefaultVisitorVoidPrinter(printer, visitorType) },
                cfirTransformerType to { printer, visitorType -> TransformerPrinter(printer, visitorType, model.rootElement) },
            )
        )
    }
}
