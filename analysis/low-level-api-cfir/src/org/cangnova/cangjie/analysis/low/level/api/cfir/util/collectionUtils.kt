/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.util

/**
 * 将 [value] 追加到 [element] 对应的可变列表中。
 */
@Suppress("NOTHING_TO_INLINE")
internal inline fun <K, V> MutableMap<K, MutableList<V>>.addValueFor(element: K, value: V) {
    getOrPut(element) { mutableListOf() } += value
}

/**
 * 将当前列表中第一个 [from] 元素替换为 [to]。
 */
internal fun <T> MutableList<T>.replaceCfirst(from: T, to: T) {
    val index = indexOf(from)
    if (index < 0) {
        error("$from was not found in $this")
    }
    set(index, to)
}
