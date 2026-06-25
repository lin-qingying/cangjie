/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.cfir.tree.generator.util

import org.cangnova.cangjie.cfir.tree.generator.BASE_PACKAGE
import org.cangnova.cangjie.generators.tree.ClassRef
import org.cangnova.cangjie.generators.tree.PositionTypeParameterRef
import org.cangnova.cangjie.generators.tree.TypeKind

/**
 * 构造位于 CFIR 基础包下的生成类型引用。
 */
fun generatedType(type: String, kind: TypeKind = TypeKind.Class): ClassRef<PositionTypeParameterRef> = generatedType("", type, kind)

/**
 * 构造位于 CFIR 指定子包下的生成类型引用。
 */
fun generatedType(packageName: String, type: String, kind: TypeKind = TypeKind.Class): ClassRef<PositionTypeParameterRef> {
    val realPackage = BASE_PACKAGE + if (packageName.isNotBlank()) ".$packageName" else ""
    return type(realPackage, type, exactPackage = true, kind = kind)
}

/**
 * 构造生成器可使用的普通类型引用。
 */
fun type(
    packageName: String,
    type: String,
    exactPackage: Boolean = false,
    kind: TypeKind = TypeKind.Interface,
): ClassRef<PositionTypeParameterRef> {
    val realPackage = if (exactPackage) packageName else packageName.let { "org.cangnova.cangjie.cfir.$it" }
    return org.cangnova.cangjie.generators.tree.type(realPackage, type, kind)
}

/**
 * 通过 reified 类型创建生成器类型引用。
 */
inline fun <reified T : Any> type() = org.cangnova.cangjie.generators.tree.type<T>()
