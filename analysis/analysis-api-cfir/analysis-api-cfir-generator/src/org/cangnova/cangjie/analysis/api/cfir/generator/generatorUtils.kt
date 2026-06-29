/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.api.cfir.generator

import org.cangnova.cangjie.utils.SmartPrinter
import kotlin.reflect.KClass
import kotlin.reflect.KType

/**
 * 以短名形式输出反射类型。
 *
 * 当 [shouldRenderFqName] 返回 true 时保留全限定名，用于规避生成源码中的简单名冲突。
 */
internal fun SmartPrinter.printTypeWithShortNames(type: KType, shouldRenderFqName: (KType) -> Boolean = { false }) {
    fun typeConversion(type: KType): String {
        val nullableSuffix = if (type.isMarkedNullable) "?" else ""
        val simpleName = if (shouldRenderFqName(type)) {
            type.qualifiedName
        } else {
            type.simpleName
        }
        return if (type.arguments.isEmpty()) simpleName + nullableSuffix
        else simpleName + type.arguments.joinToString(separator = ", ", prefix = "<", postfix = ">") {
            when (val typeArgument = it.type) {
                null -> "*"
                else -> typeConversion(typeArgument)
            } + nullableSuffix
        }
    }
    print(typeConversion(type))
}

/**
 * 反射类型对应分类器的简单名。
 */
val KType.simpleName: String
    get() = (classifier as KClass<*>).simpleName!!

/**
 * 反射类型对应分类器的全限定名。
 */
val KType.qualifiedName: String
    get() = (classifier as KClass<*>).qualifiedName!!
