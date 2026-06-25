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

/**
 * CFIR transformer 接口源码打印器。
 */
internal class TransformerPrinter(
    printer: ImportCollectingPrinter,
    /**
     * 当前要生成的 transformer 类型。
     */
    override val visitorType: ClassRef<*>,
    /**
     * CFIR 根元素，用于 transformer 返回类型和父 visitor。
     */
    private val rootElement: Element,
) : AbstractTransformerPrinter<Element, Field>(printer) {
    /**
     * transformer visitor 的类型参数列表。
     */
    override val visitorTypeParameters: List<TypeVariable>
        get() = listOf(dataTypeVariable)

    /**
     * transformer 方法的数据参数类型。
     */
    override val visitorDataType: TypeRef
        get() = dataTypeVariable

    /**
     * transformer 继承的基础 visitor 类型。
     */
    override val visitorSuperTypes: List<ClassRef<PositionTypeParameterRef>>
        get() = listOf(cfirVisitorType.withArgs(rootElement, visitorDataType))

    /**
     * 是否允许 visitor 方法声明自己的类型参数。
     */
    override val allowTypeParametersInVisitorMethods: Boolean
        get() = true

    /**
     * 返回当前元素在 transformer visitor 中的父分派元素。
     */
    override fun parentInVisitor(element: Element): Element? =
        if (element.isRootElement) null else rootElement
}
