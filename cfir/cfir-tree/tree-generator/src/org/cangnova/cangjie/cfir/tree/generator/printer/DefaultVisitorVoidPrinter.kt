package org.cangnova.cangjie.cfir.tree.generator.printer

import org.cangnova.cangjie.cfir.tree.generator.cfirVisitorVoidType
import org.cangnova.cangjie.cfir.tree.generator.model.Element
import org.cangnova.cangjie.cfir.tree.generator.model.Field
import org.cangnova.cangjie.generators.tree.AbstractVisitorPrinter
import org.cangnova.cangjie.generators.tree.ClassRef
import org.cangnova.cangjie.generators.tree.PositionTypeParameterRef
import org.cangnova.cangjie.generators.tree.StandardTypes
import org.cangnova.cangjie.generators.tree.TypeRef
import org.cangnova.cangjie.generators.tree.TypeVariable
import org.cangnova.cangjie.generators.tree.printer.ImportCollectingPrinter
import org.cangnova.cangjie.generators.util.printBlock

internal class DefaultVisitorVoidPrinter(
    printer: ImportCollectingPrinter,
    override val visitorType: ClassRef<*>,
) : AbstractVisitorPrinter<Element, Field>(printer) {
    override val visitorTypeParameters: List<TypeVariable>
        get() = emptyList()

    override val visitorDataType: TypeRef
        get() = StandardTypes.nothing.copy(nullable = true)

    override fun visitMethodReturnType(element: Element): TypeRef = StandardTypes.unit

    override val visitorSuperTypes: List<ClassRef<PositionTypeParameterRef>>
        get() = listOf(cfirVisitorVoidType)

    override val allowTypeParametersInVisitorMethods: Boolean
        get() = true

    override fun printMethodsForElement(element: Element) {
        val parentInVisitor = element.parentInVisitor ?: return
        printer.run {
            printVisitMethodDeclaration(
                element,
                hasDataParameter = false,
                override = true,
            )
            printBlock {
                println(parentInVisitor.visitFunctionName, "(", element.visitorParameterName, ")")
            }
            println()
        }
    }
}
