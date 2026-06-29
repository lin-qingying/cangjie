/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.generators.tree

/**
 * 字段容器抽象。
 */
interface FieldContainer<out Field : AbstractField<*>> {

    /**
     * 当前容器最终暴露给生成器的全部字段。
     *
     * 对元素来说这里通常包含自身声明字段以及从父元素继承来的字段；对实现类和构建器来说，
     * 这里表示打印构造函数、属性、访问器和遍历方法时需要参与计算的字段集合。
     */
    val allFields: List<Field>

    /**
     * 按字段名查找字段。
     *
     * @param fieldName DSL 或生成逻辑中使用的字段名。
     * @return 找到的字段；如果当前容器不存在该字段则返回 `null`。
     */
    fun getOrNull(fieldName: String): Field? {
        return allFields.firstOrNull { it.name == fieldName }
    }

    /**
     * 按字段名查找字段，并在字段不存在时报告带有现有字段列表的配置错误。
     *
     * 该访问器用于生成器配置阶段，字段名写错应立即失败，而不是在生成出的源码中暴露为更隐晦的问题。
     *
     * @param fieldName 必须存在于 [allFields] 中的字段名。
     * @return 对应的字段描述。
     */
    operator fun get(fieldName: String): Field {
        val result = getOrNull(fieldName)
        requireNotNull(result) {
            "Field \"$fieldName\" not found in fields of $this\nExisting fields:\n" +
                    allFields.joinToString(separator = "\n  ", prefix = "  ") { it.name }
        }
        return result
    }

    /**
     * 当前容器是否生成面向普通 visitor 的 `accept` 入口方法。
     *
     * 默认字段容器不生成入口方法，具体元素或实现类可根据树模型需求覆盖。
     */
    val hasAcceptMethod: Boolean
        get() = false

    /**
     * 当前容器是否生成面向 transformer 的 `transform` 入口方法。
     *
     * 默认字段容器不生成入口方法，具体元素或实现类可根据树模型需求覆盖。
     */
    val hasTransformMethod: Boolean
        get() = false

    /**
     * 是否在当前容器上生成遍历子节点的 `acceptChildren` 方法。
     *
     * 该标志由模型后处理阶段根据子元素分布和可遍历字段自动推导，也可以被特定配置覆盖。
     */
    var hasAcceptChildrenMethod: Boolean

    /**
     * 是否在当前容器上生成转换子节点的 `transformChildren` 方法。
     *
     * 只有可变字段或列表字段才会参与转换，因此该标志与 [transformableChildren] 的结果共同决定方法体。
     */
    var hasTransformChildrenMethod: Boolean

    /**
     * 子节点遍历顺序覆盖列表。
     *
     * 当不为 `null` 时，出现在列表中的字段会按给定顺序提前；未列出的字段保留在尾部。
     */
    val childrenOrderOverride: List<String>?
        get() = null

    /**
     * visitor 需要递归访问的子字段。
     *
     * 只包含语义上是树子节点、且不依赖计算 getter 提供默认值的元素字段，返回值会应用
     * [childrenOrderOverride] 中定义的顺序。
     */
    val walkableChildren: List<Field>
        get() = allFields
            .filter {
                it.containsElement && it.isChild
                        && it.implementationDefaultStrategy?.withGetter != true
            }
            .reorderFieldsIfNecessary(childrenOrderOverride)

    /**
     * transformer 需要递归转换的子字段。
     *
     * 转换要求字段可被重新赋值，或者字段本身是列表字段以便替换列表内容；其他只读字段只参与访问不参与转换。
     */
    val transformableChildren: List<Field>
        get() = walkableChildren.filter { it.isMutable || it is ListField }
}

/**
 * 按给定顺序重排字段列表；未出现在顺序列表中的字段保持在末尾。
 */
internal fun <Field : AbstractField<*>> List<Field>.reorderFieldsIfNecessary(order: List<String>?): List<Field> =
    if (order == null) {
        this
    } else {
        sortedBy {
            val position = order.indexOf(it.name)
            if (position < 0) order.size else position
        }
    }
