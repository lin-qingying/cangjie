/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.cfir.tree.generator.model

import org.cangnova.cangjie.cfir.tree.generator.BASE_PACKAGE
import org.cangnova.cangjie.generators.tree.*
import org.cangnova.cangjie.generators.tree.ElementOrRef as GenericElementOrRef
import org.cangnova.cangjie.generators.tree.ElementRef as GenericElementRef

/**
 * CFIR tree 生成器中的元素元模型。
 */
class Element(name: String, override val propertyName: String, kind: Kind) : AbstractElement<Element, Field, Implementation>(name) {
    /**
     * 元素允许选择的实现形态集合。
     */
    companion object {
        /**
         * CFIR 元素生成器允许的接口/抽象类实现种类。
         */
        private val allowedKinds = setOf(
            ImplementationKind.Interface,
            ImplementationKind.SealedInterface,
            ImplementationKind.AbstractClass,
            ImplementationKind.SealedClass,
        )
    }

    /**
     * 生成类名使用的统一前缀。
     */
    override val namePrefix: String
        get() = "Cfir"

    /**
     * 当前元素生成源码所在包名。
     */
    override val packageName: String = BASE_PACKAGE + kind.packageName.let { if (it.isBlank()) it else ".$it" }

    /**
     * 当前元素生成的实现类型种类。
     */
    override var kind: ImplementationKind?
        get() = super.kind
        set(value) {
            if (value !in allowedKinds) {
                throw IllegalArgumentException(value.toString())
            }
            super.kind = value
        }

    /**
     * 当前元素是否显式要求生成 transformOtherChildren 钩子。
     */
    var _needTransformOtherChildren: Boolean = false

    /**
     * CFIR 元素始终生成 accept 方法。
     */
    override val hasAcceptMethod: Boolean
        get() = true

    /**
     * CFIR 元素始终生成 transform 方法。
     */
    override val hasTransformMethod: Boolean
        get() = true

    /**
     * 当前模型层不直接声明 walkable children，由字段配置决定。
     */
    override val walkableChildren: List<Field>
        get() = emptyList()

    /**
     * 当前模型层不直接声明 transformable children，由字段配置决定。
     */
    override val transformableChildren: List<Field>
        get() = emptyList()

    /**
     * visitor 方法中当前元素参数的名称。
     */
    override val visitorParameterName: String
        get() = safeDecapitalizedName

    /**
     * 当前元素或任一父元素是否需要 transformOtherChildren。
     */
    val needTransformOtherChildren: Boolean
        get() = _needTransformOtherChildren || elementParents.any { it.element.needTransformOtherChildren }

    /**
     * 向当前元素追加字段集合的复制实例。
     */
    operator fun FieldSet.unaryPlus() {
        val copiedFields = fieldDefinitions.map { it.copy() }
        this@Element.fields.addAll(copiedFields)
    }

    /**
     * CFIR 元素所属的生成包类别。
     */
    enum class Kind(val packageName: String) {
        Expression("expressions"),
        Declaration("declarations"),
        Pattern("patterns"),
        Reference("references"),
        TypeRef("types"),
        Contracts("contracts"),
        Diagnostics("diagnostics"),
        Other(""),
    }
}

/**
 * 绑定 CFIR 元素类型后的元素引用别名。
 */
typealias ElementRef = GenericElementRef<Element>

/**
 * 绑定 CFIR 元素类型后的元素或引用联合别名。
 */
typealias ElementOrRef = GenericElementOrRef<Element>
