/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.generators.tree

import org.cangnova.cangjie.generators.tree.imports.ImportCollecting
import org.cangnova.cangjie.generators.tree.imports.Importable

/**
 * 树节点的非抽象实现类。
 */
@Suppress("LeakingThis")
abstract class AbstractImplementation<Implementation, Element, Field>(
    /**
     * 该实现类对应的抽象树元素。
     */
    val element: Element,
    /**
     * 显式实现类名。
     *
     * 为 `null` 时默认使用 `${element.typeName}Impl`。
     */
    val name: String?,
) : FieldContainer<Field>, ImplementationKindOwner
        where Implementation : AbstractImplementation<Implementation, Element, Field>,
              Element : AbstractElement<Element, Field, Implementation>,
              Field : AbstractField<Field> {

    /**
     * 实现类在实现层级中的父元素列表。
     */
    override val allParents: List<Element>
        get() = listOf(element)

    /**
     * 实现类沿用元素体系的类型名前缀。
     */
    val namePrefix: String
        get() = element.namePrefix

    /**
     * 生成源码中的实现类类型名。
     */
    override val typeName: String
        get() = name ?: (element.typeName + "Impl")

    /**
     * 渲染实现类类型名，并在元素存在类型参数时渲染同名类型参数列表。
     */
    override fun renderTo(appendable: Appendable, importCollector: ImportCollecting) {
        importCollector.addImport(this)
        appendable.append(this.typeName)
        if (element.params.isNotEmpty()) {
            element.params.joinTo(appendable, prefix = "<", postfix = ">") { it.name }
        }
    }

    /**
     * 实现类定义自身不受类型参数替换影响。
     */
    override fun substitute(map: TypeParameterSubstitutionMap) = this

    /**
     * 实现类生成文件所在包名。
     */
    override val packageName = element.packageName + ".impl"

    /**
     * 实现类生成源码需要额外输出的导入。
     */
    val additionalImports = mutableListOf<Importable>()

    /**
     * 生成到实现类上的 KDoc 文本。
     */
    var kDoc: String? = null

    init {
        @Suppress("UNCHECKED_CAST")
        element.implementations += this as Implementation
    }

    /**
     * 当前实现类是否生成 `acceptChildren` 方法。
     *
     * 接口和抽象类不生成具体 children 方法体，普通实现类默认生成。
     */
    override var hasAcceptChildrenMethod: Boolean = false
        get() {
            val isInterface = kind == ImplementationKind.Interface || kind == ImplementationKind.SealedInterface
            val isAbstract = kind == ImplementationKind.AbstractClass || kind == ImplementationKind.SealedClass
            return !isInterface && !isAbstract
        }

    /**
     * 当前实现类是否生成 `transformChildren` 方法。
     */
    override var hasTransformChildrenMethod: Boolean = true

    /**
     * 实现类类型本身是否使用 public 可见性。
     */
    var isPublic = false

    /**
     * 实现类主构造函数是否使用 public 可见性。
     */
    var isConstructorPublic = true

    /**
     * 是否把 implementation opt-in 注解放到构造函数上。
     */
    var putImplementationOptInInConstructor = true

    /**
     * 构造函数参数顺序覆盖列表。
     */
    var constructorParameterOrderOverride: List<String>? = null

    /**
     * 判断字段是否由实现类体内默认值或自定义 setter 处理，而不是必须作为构造参数传入。
     */
    private fun withDefault(field: Field) =
        !field.isFinal && field.implementationDefaultStrategy !is AbstractField.ImplementationDefaultStrategy.Required

    /**
     * 应放入主构造函数参数列表的字段。
     */
    val fieldsInConstructor by lazy { allFields.filter { !withDefault(it) } }

    /**
     * 应放入类体的字段。
     */
    val fieldsInBody by lazy { allFields.filter { withDefault(it) || it.customSetter != null } }

    /**
     * 当前实现类是否需要 opt-in 注解保护。
     */
    var requiresOptIn = false

    /**
     * 当前实现类最终推导或显式配置的实现种类。
     *
     * 设置为可生成叶子 Builder 的种类时，会同步创建或保留 [builder]。
     */
    override var kind: ImplementationKind? = null
        set(value) {
            field = value
            if (kind != ImplementationKind.FinalClass) {
                isPublic = true
            }
            @Suppress("UNCHECKED_CAST")
            builder = if (value?.hasLeafBuilder == true) {
                builder ?: LeafBuilder(this as Implementation)
            } else {
                null
            }
        }

    /**
     * 与当前实现类关联的叶子 Builder。
     */
    var builder: LeafBuilder<Field, Element, Implementation>? = null

    /**
     * 当前实现类是否应写入生成输出。
     */
    open val doPrint: Boolean
        get() = true

    /**
     * 返回实现类类型名，便于配置错误与调试输出。
     */
    override fun toString(): String = buildString { renderTo(this, ImportCollecting.Empty) }
}
