package org.cangnova.cangjie.cfir.tree.generator.context

import org.cangnova.cangjie.cfir.tree.generator.model.Element
import org.cangnova.cangjie.cfir.tree.generator.model.Field
import org.cangnova.cangjie.cfir.tree.generator.model.Implementation
import org.cangnova.cangjie.generators.tree.config.AbstractImplementationConfigurator

abstract class AbstractCfirTreeImplementationConfigurator :
    AbstractImplementationConfigurator<Implementation, Element, Field>() {
    final override fun createImplementation(element: Element, name: String?): Implementation =
        Implementation(element, name)
    protected fun ImplementationContext.noSource() {
        defaultNull("source", withGetter = true)
    }
}

