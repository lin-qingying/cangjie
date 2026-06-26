/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.utils.errors

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
/**
 * 要求对象是指定类型，成功返回后通过 contract 暴露类型收窄。
 */
public inline fun <reified T> requireIsInstance(obj: Any) {
    contract {
        returns() implies (obj is T)
    }
    require(obj is T) { "Expected ${T::class} instead of ${obj::class} for $obj" }
}

@OptIn(ExperimentalContracts::class)
/**
 * 检查对象是指定类型，失败表示内部不变量被破坏。
 */
public inline fun <reified T> checkIsInstance(obj: Any) {
    contract {
        returns() implies (obj is T)
    }
    check(obj is T) { "Expected ${T::class} instead of ${obj::class} for $obj" }
}
