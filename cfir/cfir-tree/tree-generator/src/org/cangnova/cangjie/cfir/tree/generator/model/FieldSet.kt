/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.cfir.tree.generator.model

/**
 * 可复用字段定义集合。
 */
data class FieldSet(
    /**
     * 当前集合包含的字段定义。
     */
    val fieldDefinitions: List<Field>
) {
    /**
     * 复制字段集合并对每个字段应用配置。
     */
    operator fun invoke(config: Field.() -> Unit): FieldSet {
        val configured = fieldDefinitions.map { it.copy().apply(config) }
        return FieldSet(configured)
    }
}

/**
 * 构造字段集合。
 */
fun fieldSet(vararg fields: Field): FieldSet = FieldSet(fields.toList())
