package org.cangjie.cfir.tree.generator.printer

import org.cangjie.cfir.tree.generator.cfirVisitorVoidType
import org.cangjie.cfir.tree.generator.model.Element
import org.cangjie.cfir.tree.generator.model.Field
import org.cangjie.generators.tree.AbstractVisitorPrinter
import org.cangjie.generators.tree.ClassRef
import org.cangjie.generators.tree.PositionTypeParameterRef
import org.cangjie.generators.tree.StandardTypes
import org.cangjie.generators.tree.TypeRef
import org.cangjie.generators.tree.TypeVariable
import org.cangjie.generators.tree.printer.ImportCollectingPrinter
import org.cangjie.generators.util.printBlock

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
