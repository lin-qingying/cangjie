package org.cangnova.cangjie.cfir.tree.generator.printer

import org.cangnova.cangjie.cfir.tree.generator.model.Element
import org.cangnova.cangjie.cfir.tree.generator.model.Field
import org.cangnova.cangjie.cfir.tree.generator.cfirVisitorType
import org.cangnova.cangjie.generators.tree.AbstractTransformerPrinter
import org.cangnova.cangjie.generators.tree.ClassRef
import org.cangnova.cangjie.generators.tree.PositionTypeParameterRef
import org.cangnova.cangjie.generators.tree.TypeRef
import org.cangnova.cangjie.generators.tree.TypeVariable
import org.cangnova.cangjie.generators.tree.withArgs
import org.cangnova.cangjie.generators.tree.printer.ImportCollectingPrinter

internal class TransformerPrinter(
    printer: ImportCollectingPrinter,
    override val visitorType: ClassRef<*>,
    private val rootElement: Element,
) : AbstractTransformerPrinter<Element, Field>(printer) {
    override val visitorTypeParameters: List<TypeVariable>
        get() = listOf(dataTypeVariable)

    override val visitorDataType: TypeRef
        get() = dataTypeVariable

    override val visitorSuperTypes: List<ClassRef<PositionTypeParameterRef>>
        get() = listOf(cfirVisitorType.withArgs(rootElement, visitorDataType))

    override val allowTypeParametersInVisitorMethods: Boolean
        get() = true

    override fun parentInVisitor(element: Element): Element? =
        if (element.isRootElement) null else rootElement
}
