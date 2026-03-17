/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.generators.tree

/**
 * 字段容器抽象。
 */
interface FieldContainer<out Field : AbstractField<*>> {

    val allFields: List<Field>

    fun getOrNull(fieldName: String): Field? {
        return allFields.firstOrNull { it.name == fieldName }
    }

    operator fun get(fieldName: String): Field {
        val result = getOrNull(fieldName)
        requireNotNull(result) {
            "Field \"$fieldName\" not found in fields of $this\nExisting fields:\n" +
                    allFields.joinToString(separator = "\n  ", prefix = "  ") { it.name }
        }
        return result
    }

    val hasAcceptMethod: Boolean
        get() = false

    val hasTransformMethod: Boolean
        get() = false

    var hasAcceptChildrenMethod: Boolean

    var hasTransformChildrenMethod: Boolean

    val childrenOrderOverride: List<String>?
        get() = null

    val walkableChildren: List<Field>
        get() = allFields
            .filter {
                it.containsElement && it.isChild
                        && it.implementationDefaultStrategy?.withGetter != true
            }
            .reorderFieldsIfNecessary(childrenOrderOverride)

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
