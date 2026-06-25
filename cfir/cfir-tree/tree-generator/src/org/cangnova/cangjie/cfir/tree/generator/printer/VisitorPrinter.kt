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

/**
 * CFIR 泛型 visitor 接口源码打印器。
 */
internal class VisitorPrinter(
    printer: ImportCollectingPrinter,
    /**
     * 当前要生成的 visitor 类型。
     */
    override val visitorType: ClassRef<*>,
    /**
     * 是否默认沿父 visitor 方法分派。
     */
    private val visitSuperTypeByDefault: Boolean,
) : AbstractVisitorPrinter<Element, Field>(printer) {
    /**
     * visitor 的返回值和数据类型参数。
     */
    override val visitorTypeParameters: List<TypeVariable>
        get() = listOf(resultTypeVariable, dataTypeVariable)

    /**
     * 当前 visitor 继承的父 visitor 类型。
     */
    override val visitorSuperTypes: List<ClassRef<PositionTypeParameterRef>> =
        listOfNotNull(cfirVisitorType.takeIf { visitSuperTypeByDefault }?.withArgs(resultTypeVariable, dataTypeVariable))

    /**
     * visitor 方法的数据参数类型。
     */
    override val visitorDataType: TypeRef
        get() = dataTypeVariable

    /**
     * 单个 visit 方法的返回类型。
     */
    override fun visitMethodReturnType(element: Element): TypeRef = resultTypeVariable

    /**
     * 是否允许 visitor 方法声明自己的类型参数。
     */
    override val allowTypeParametersInVisitorMethods: Boolean
        get() = true

    /**
     * 判断某元素是否跳过方法生成。
     */
    override fun skipElement(element: Element): Boolean = visitSuperTypeByDefault && element.isRootElement

    /**
     * 返回当前元素在 visitor 中的父分派元素。
     */
    override fun parentInVisitor(element: Element): Element? = when {
        element.isRootElement -> null
        visitSuperTypeByDefault -> element.parentInVisitor
        else -> CfirTree.rootElement
    }
}
