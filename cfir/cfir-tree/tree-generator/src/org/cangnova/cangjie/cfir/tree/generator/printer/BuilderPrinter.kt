package org.cangjie.cfir.tree.generator.printer

import org.cangjie.cfir.tree.generator.cfirBuilderDslAnnotation
import org.cangjie.cfir.tree.generator.cfirImplementationDetailType
import org.cangjie.cfir.tree.generator.model.Element
import org.cangjie.cfir.tree.generator.model.Field
import org.cangjie.cfir.tree.generator.model.Implementation
import org.cangjie.generators.tree.AbstractBuilderPrinter
import org.cangjie.generators.tree.ClassRef
import org.cangjie.generators.tree.printer.ImportCollectingPrinter

internal class BuilderPrinter(printer: ImportCollectingPrinter) :
    AbstractBuilderPrinter<Element, Implementation, Field>(printer) {
    override val implementationDetailAnnotation: ClassRef<*>
        get() = cfirImplementationDetailType

    override val builderDslAnnotation: ClassRef<*>
        get() = cfirBuilderDslAnnotation
}

