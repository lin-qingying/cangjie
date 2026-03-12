package org.cangjie.cfir.tree.generator.context

import org.cangjie.cfir.tree.generator.model.Element
import org.cangjie.cfir.tree.generator.model.Field
import org.cangjie.cfir.tree.generator.model.Implementation
import org.cangjie.generators.tree.config.AbstractImplementationConfigurator

abstract class AbstractCfirTreeImplementationConfigurator :
    AbstractImplementationConfigurator<Implementation, Element, Field>() {
    final override fun createImplementation(element: Element, name: String?): Implementation =
        Implementation(element, name)
}

