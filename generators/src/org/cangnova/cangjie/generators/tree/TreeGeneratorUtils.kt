/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangjie.generators.tree

/**
 * Appendable 版的 joinToWithBuffer，与 Kotlin 编译器 addToStdlib 中的签名对齐。
 * lambda 的 receiver 是 Appendable，参数是元素。
 */
inline fun <T, A : Appendable> Iterable<T>.joinToWithBuffer(
    buffer: A,
    separator: CharSequence = ", ",
    prefix: CharSequence = "",
    postfix: CharSequence = "",
    action: A.(T) -> Unit,
): A {
    buffer.append(prefix)
    var first = true
    for (element in this) {
        if (!first) buffer.append(separator)
        first = false
        buffer.action(element)
    }
    buffer.append(postfix)
    return buffer
}
