package org.cangnova.cangjie.cfir.tree.generator.printer

import org.cangnova.cangjie.cfir.tree.generator.CfirTree
import org.cangnova.cangjie.cfir.tree.generator.cfirVisitorType
import org.cangnova.cangjie.cfir.tree.generator.model.Element
import org.cangnova.cangjie.cfir.tree.generator.model.Field
import org.cangnova.cangjie.generators.tree.AbstractVisitorPrinter
import org.cangnova.cangjie.generators.tree.ClassRef
import org.cangnova.cangjie.generators.tree.PositionTypeParameterRef
import org.cangnova.cangjie.generators.tree.TypeRef
import org.cangnova.cangjie.generators.tree.TypeVariable
import org.cangnova.cangjie.generators.tree.withArgs
import org.cangnova.cangjie.generators.tree.printer.ImportCollectingPrinter

internal class VisitorPrinter(
    printer: ImportCollectingPrinter,
    override val visitorType: ClassRef<*>,
    private val visitSuperTypeByDefault: Boolean,
) : AbstractVisitorPrinter<Element, Field>(printer) {
    override val visitorTypeParameters: List<TypeVariable>
        get() = listOf(resultTypeVariable, dataTypeVariable)

    override val visitorSuperTypes: List<ClassRef<PositionTypeParameterRef>> =
        listOfNotNull(cfirVisitorType.takeIf { visitSuperTypeByDefault }?.withArgs(resultTypeVariable, dataTypeVariable))

    override val visitorDataType: TypeRef
        get() = dataTypeVariable

    override fun visitMethodReturnType(element: Element): TypeRef = resultTypeVariable

    override val allowTypeParametersInVisitorMethods: Boolean
        get() = true

    override fun skipElement(element: Element): Boolean = visitSuperTypeByDefault && element.isRootElement

    override fun parentInVisitor(element: Element): Element? = when {
        element.isRootElement -> null
        visitSuperTypeByDefault -> element.parentInVisitor
        else -> CfirTree.rootElement
    }
}
