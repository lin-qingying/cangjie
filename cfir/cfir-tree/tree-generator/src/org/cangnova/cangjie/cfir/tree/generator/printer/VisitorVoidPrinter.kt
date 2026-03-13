package org.cangnova.cangjie.cfir.tree.generator.printer

import org.cangnova.cangjie.cfir.tree.generator.CfirTree
import org.cangnova.cangjie.cfir.tree.generator.cfirVisitorType
import org.cangnova.cangjie.cfir.tree.generator.model.Element
import org.cangnova.cangjie.cfir.tree.generator.model.Field
import org.cangnova.cangjie.generators.tree.AbstractVisitorVoidPrinter
import org.cangnova.cangjie.generators.tree.ClassRef
import org.cangnova.cangjie.generators.tree.PositionTypeParameterRef
import org.cangnova.cangjie.generators.tree.printer.ImportCollectingPrinter

internal class VisitorVoidPrinter(
    printer: ImportCollectingPrinter,
    override val visitorType: ClassRef<*>,
) : AbstractVisitorVoidPrinter<Element, Field>(printer) {
    override val visitorSuperClass: ClassRef<PositionTypeParameterRef>
        get() = cfirVisitorType

    override val allowTypeParametersInVisitorMethods: Boolean
        get() = true

    override val useAbstractMethodForRootElement: Boolean
        get() = true

    override val overriddenVisitMethodsAreFinal: Boolean
        get() = true

    override fun parentInVisitor(element: Element): Element = CfirTree.rootElement
}
