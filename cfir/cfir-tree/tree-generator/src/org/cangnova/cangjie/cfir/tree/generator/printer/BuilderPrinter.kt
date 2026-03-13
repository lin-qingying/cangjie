package org.cangnova.cangjie.cfir.tree.generator.printer

import org.cangnova.cangjie.cfir.tree.generator.cfirBuilderDslAnnotation
import org.cangnova.cangjie.cfir.tree.generator.cfirImplementationDetailType
import org.cangnova.cangjie.cfir.tree.generator.model.Element
import org.cangnova.cangjie.cfir.tree.generator.model.Field
import org.cangnova.cangjie.cfir.tree.generator.model.Implementation
import org.cangnova.cangjie.generators.tree.AbstractBuilderPrinter
import org.cangnova.cangjie.generators.tree.ClassRef
import org.cangnova.cangjie.generators.tree.printer.ImportCollectingPrinter

internal class BuilderPrinter(printer: ImportCollectingPrinter) :
    AbstractBuilderPrinter<Element, Implementation, Field>(printer) {
    override val implementationDetailAnnotation: ClassRef<*>
        get() = cfirImplementationDetailType

    override val builderDslAnnotation: ClassRef<*>
        get() = cfirBuilderDslAnnotation
}

