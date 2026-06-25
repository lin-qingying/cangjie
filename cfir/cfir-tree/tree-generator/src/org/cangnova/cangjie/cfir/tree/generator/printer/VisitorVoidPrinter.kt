package org.cangnova.cangjie.cfir.tree.generator.printer

import org.cangnova.cangjie.cfir.tree.generator.CfirTree
import org.cangnova.cangjie.cfir.tree.generator.cfirVisitorType
import org.cangnova.cangjie.cfir.tree.generator.model.Element
import org.cangnova.cangjie.cfir.tree.generator.model.Field
import org.cangnova.cangjie.generators.tree.AbstractVisitorVoidPrinter
import org.cangnova.cangjie.generators.tree.ClassRef
import org.cangnova.cangjie.generators.tree.PositionTypeParameterRef
import org.cangnova.cangjie.generators.tree.printer.ImportCollectingPrinter

/**
 * 无返回值 CFIR visitor 接口源码打印器。
 */
internal class VisitorVoidPrinter(
    printer: ImportCollectingPrinter,
    /**
     * 当前要生成的 visitor 类型。
     */
    override val visitorType: ClassRef<*>,
) : AbstractVisitorVoidPrinter<Element, Field>(printer) {
    /**
     * 无返回值 visitor 继承的泛型 visitor 类型。
     */
    override val visitorSuperClass: ClassRef<PositionTypeParameterRef>
        get() = cfirVisitorType

    /**
     * 是否允许 visitor 方法声明自己的类型参数。
     */
    override val allowTypeParametersInVisitorMethods: Boolean
        get() = true

    /**
     * 根元素 visit 方法是否生成为抽象方法。
     */
    override val useAbstractMethodForRootElement: Boolean
        get() = true

    /**
     * 重写的 visit 方法是否生成为 final。
     */
    override val overriddenVisitMethodsAreFinal: Boolean
        get() = true

    /**
     * 返回所有非根元素在 void visitor 中的父分派元素。
     */
    override fun parentInVisitor(element: Element): Element = CfirTree.rootElement
}
