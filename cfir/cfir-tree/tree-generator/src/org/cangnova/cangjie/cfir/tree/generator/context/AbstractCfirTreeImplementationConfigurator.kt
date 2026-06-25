package org.cangnova.cangjie.cfir.tree.generator.context

import org.cangnova.cangjie.cfir.tree.generator.model.Element
import org.cangnova.cangjie.cfir.tree.generator.model.Field
import org.cangnova.cangjie.cfir.tree.generator.model.Implementation
import org.cangnova.cangjie.generators.tree.config.AbstractImplementationConfigurator

/**
 * CFIR tree 实现类配置器基类。
 */
abstract class AbstractCfirTreeImplementationConfigurator :
    AbstractImplementationConfigurator<Implementation, Element, Field>() {
    /**
     * 创建 CFIR 元素实现元模型实例。
     */
    final override fun createImplementation(element: Element, name: String?): Implementation =
        Implementation(element, name)
    /**
     * 将实现类的 source 字段默认生成为 null。
     */
    protected fun ImplementationContext.noSource() {
        defaultNull("source", withGetter = true)
    }
}
