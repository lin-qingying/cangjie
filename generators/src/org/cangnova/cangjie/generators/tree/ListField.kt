/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangjie.generators.tree

/**
 * 表示存储任意元素列表的字段。
 */
interface ListField {

    /**
     * 列表的元素类型。
     */
    val baseType: TypeRef

    /**
     * 字段的列表类型，例如 [List] 或 [MutableList]。
     */
    val listType: ClassRef<PositionTypeParameterRef>

    val typeRef: ClassRef<PositionTypeParameterRef>
        get() = listType.withArgs(baseType)
}
