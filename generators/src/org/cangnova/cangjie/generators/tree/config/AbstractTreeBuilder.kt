/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.generators.tree.config

import org.cangnova.cangjie.generators.tree.*
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * 抽象树元素配置器。
 *
 * 提供 DSL 用于声明元素、父子关系与类型参数，并最终构建 [Model]。
 */
abstract class AbstractElementConfigurator<Element, Field, Category>
        where Element : AbstractElement<Element, Field, *>,
              Field : AbstractField<Field> {

    /**
     * 元素 DSL 属性委托。
     *
     * 通过 `val expression by element(...)` 语法延迟创建元素，并在 [build] 阶段应用初始化逻辑。
     */
    inner class ElementDelegate(
        /**
         * 当前元素所属的业务分类。
         */
        private val category: Category,
        /**
         * 显式元素名；为 `null` 时由委托属性名推导。
         */
        private val name: String?,
        /**
         * 当前元素是否声明为 sealed。
         */
        private val isSealed: Boolean,
    ) : ReadOnlyProperty<AbstractElementConfigurator<Element, Field, Category>, Element>,
        PropertyDelegateProvider<AbstractElementConfigurator<Element, Field, Category>, ElementDelegate> {

        /**
         * 委托实际创建的元素实例。
         */
        var element: Element? = null
            private set

        /**
         * 返回已经由 [provideDelegate] 创建的元素实例。
         */
        override fun getValue(thisRef: AbstractElementConfigurator<Element, Field, Category>, property: KProperty<*>): Element {
            return element!!
        }

        /**
         * 根据委托属性名创建元素，并把创建出的元素绑定到当前委托。
         */
        override fun provideDelegate(
            thisRef: AbstractElementConfigurator<Element, Field, Category>,
            property: KProperty<*>,
        ): ElementDelegate {
            val path = thisRef.javaClass.name + "." + property.name
            element = createElement(name ?: property.name.replaceFirstChar(Char::uppercaseChar), path, category).also {
                it.isSealed = isSealed
            }
            return this
        }
    }

    /**
     * 创建具体树模型中的元素实例。
     */
    protected abstract fun createElement(name: String, propertyName: String, category: Category): Element

    /**
     * 延迟执行的元素初始化回调列表。
     */
    private val configurationCallbacks = mutableListOf<() -> Element>()

    /**
     * 当前树模型的根元素。
     */
    abstract val rootElement: Element

    /**
     * 执行所有延迟元素配置并构建 [Model]。
     */
    fun build(): Model<Element> {
        val elements = configurationCallbacks.map { it() }
        return Model(elements, rootElement)
    }

    /**
     * 创建元素委托并注册构建阶段的初始化回调。
     */
    private fun createElement(
        category: Category,
        name: String? = null,
        isSealed: Boolean,
        initializer: Element.() -> Unit = {},
    ): ElementDelegate {
        val delegate = ElementDelegate(category, name, isSealed)
        configurationCallbacks.add {
            delegate.element!!.apply {
                initializer()
                if (elementParents.isEmpty() && this != rootElement) {
                    addParent(rootElement.toRef())
                }
            }
        }
        return delegate
    }

    /**
     * 声明一个普通元素。
     */
    fun element(category: Category, name: String? = null, initializer: Element.() -> Unit = {}): ElementDelegate =
        createElement(category, name, isSealed = false, initializer)

    /**
     * 声明一个 sealed 元素。
     */
    fun sealedElement(category: Category, name: String? = null, initializer: Element.() -> Unit = {}): ElementDelegate =
        createElement(category, name, isSealed = true, initializer)

    /**
     * 为当前元素添加一个非树元素父类型。
     */
    protected fun Element.parent(type: ClassRef<*>) {
        otherParents.add(type)
    }

    /**
     * 为当前元素添加一个树元素父类型。
     */
    protected fun Element.parent(type: ElementOrRef<Element>) {
        addParent(type.toRef())
    }

    /**
     * 创建元素类型参数。
     */
    protected fun param(name: String, vararg bounds: TypeRef, variance: Variance = Variance.INVARIANT): TypeVariable {
        return TypeVariable(name, bounds.toList(), variance)
    }

    /**
     * 元素配置 DSL 中常用的标准类型快捷引用。
     */
    companion object {
        /**
         * 标准 `Int` 类型引用。
         */
        val int = StandardTypes.int

        /**
         * 标准 `String` 类型引用。
         */
        val string = StandardTypes.string

        /**
         * 标准 `Boolean` 类型引用。
         */
        val boolean = StandardTypes.boolean
    }
}

/**
 * 无分类场景下的元素 DSL 入口。
 */
fun <Element, Field> AbstractElementConfigurator<Element, Field, Nothing?>.element(
    name: String? = null,
    initializer: Element.() -> Unit = {},
): AbstractElementConfigurator<Element, Field, Nothing?>.ElementDelegate
        where Element : AbstractElement<Element, Field, *>,
              Field : AbstractField<Field> {
    return element(null, name, initializer)
}

/**
 * 无分类场景下的密封元素 DSL 入口。
 */
fun <Element, Field> AbstractElementConfigurator<Element, Field, Nothing?>.sealedElement(
    name: String? = null,
    initializer: Element.() -> Unit = {},
): AbstractElementConfigurator<Element, Field, Nothing?>.ElementDelegate
        where Element : AbstractElement<Element, Field, *>,
              Field : AbstractField<Field> {
    return sealedElement(null, name, initializer)
}
