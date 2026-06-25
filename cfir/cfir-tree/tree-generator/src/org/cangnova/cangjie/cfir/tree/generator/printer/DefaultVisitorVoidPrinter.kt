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

/**
 * 默认无返回值 visitor 实现源码打印器。
 */
internal class DefaultVisitorVoidPrinter(
    printer: ImportCollectingPrinter,
    /**
     * 当前要生成的默认 visitor 类型。
     */
    override val visitorType: ClassRef<*>,
) : AbstractVisitorPrinter<Element, Field>(printer) {
    /**
     * 默认 void visitor 不声明额外类型参数。
     */
    override val visitorTypeParameters: List<TypeVariable>
        get() = emptyList()

    /**
     * 默认 void visitor 的 data 参数类型固定为 Nothing?。
     */
    override val visitorDataType: TypeRef
        get() = StandardTypes.nothing.copy(nullable = true)

    /**
     * 默认 void visitor 的 visit 方法返回 Unit。
     */
    override fun visitMethodReturnType(element: Element): TypeRef = StandardTypes.unit

    /**
     * 默认 void visitor 继承的基础 visitor 类型。
     */
    override val visitorSuperTypes: List<ClassRef<PositionTypeParameterRef>>
        get() = listOf(cfirVisitorVoidType)

    /**
     * 是否允许 visitor 方法声明自己的类型参数。
     */
    override val allowTypeParametersInVisitorMethods: Boolean
        get() = true

    /**
     * 打印单个元素的默认分派 visit 方法。
     */
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
