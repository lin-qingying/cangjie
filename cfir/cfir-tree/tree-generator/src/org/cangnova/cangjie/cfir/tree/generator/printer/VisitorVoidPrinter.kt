package org.cangjie.cfir.tree.generator.printer

import org.cangjie.cfir.tree.generator.CfirTree
import org.cangjie.cfir.tree.generator.cfirVisitorType
import org.cangjie.cfir.tree.generator.model.Element
import org.cangjie.cfir.tree.generator.model.Field
import org.cangjie.generators.tree.AbstractVisitorVoidPrinter
import org.cangjie.generators.tree.ClassRef
import org.cangjie.generators.tree.PositionTypeParameterRef
import org.cangjie.generators.tree.printer.ImportCollectingPrinter

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
