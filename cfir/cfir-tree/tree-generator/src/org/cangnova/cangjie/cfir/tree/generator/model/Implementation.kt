/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.cfir.tree.generator.model

import org.cangnova.cangjie.generators.tree.AbstractImplementation

/**
 * CFIR 元素具体实现类的生成元模型。
 */
class Implementation(element: Element, name: String?) : AbstractImplementation<Implementation, Element, Field>(element, name) {
    /**
     * 具体实现持有的字段复制列表。
     */
    override val allFields: List<Field> = element.allFields.map { it.copy() }
}
