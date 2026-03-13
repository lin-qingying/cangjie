package org.cangjie.cfir.tree.generator

import org.cangjie.cfir.tree.generator.printer.*
import org.cangjie.generators.tree.detectBaseTransformerTypes
import org.cangjie.generators.tree.InterfaceAndAbstractClassConfigurator
import org.cangjie.generators.tree.printer.TreeGenerator
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
        printElementImplementations(implementations, ::ImplementationPrinter)
        printElementBuilders(implementations.mapNotNull { it.builder } + builderConfigurator.intermediateBuilders, ::BuilderPrinter)
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
