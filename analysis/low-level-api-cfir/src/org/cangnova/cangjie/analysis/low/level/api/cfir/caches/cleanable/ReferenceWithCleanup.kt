/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.caches.cleanable

import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirInternals

@LLCfirInternals
/**
 * 带 key 和 cleanup 处理器的 value reference 抽象。
 */
interface ReferenceWithCleanup<K, V> {
    /**
     * 该 reference 在 cache backing map 中对应的 key。
     */
    val key: K

    /**
     * referent 被移除或回收后需要调用的 cleanup 处理器。
     */
    val cleaner: ValueReferenceCleaner<V>

    /**
     * 返回当前 referent；如果已被 GC 回收则返回 null。
     */
    fun get(): V?
}

/**
 * 按 key 和仍然存活的 referent 实现 reference equality。
 */
internal fun <K, V> ReferenceWithCleanup<K, V>.equalsImpl(other: Any?): Boolean {
    // When the referent is collected, equality should be identity-based (for `processQueue` to remove this very same reference).
    // Hence, we skip the value equality check if the referent has been collected and `get()` returns `null`. If the reference is still
    // valid, this is just a canonical equals on referents for `replace(K,V,V)`.
    //
    // The `cleaner` is not part of equality, because `value` equality implies `cleaner` equivalence.
    if (this === other) return true
    if (other == null || other !is ReferenceWithCleanup<*, *>) return false
    if (key != other.key) return false

    val value = get() ?: return false
    return value == other.get()
}

/**
 * 使用 key hash 作为 reference hash，保证 collected reference 可从 map 中匹配移除。
 */
internal fun <K, V> ReferenceWithCleanup<K, V>.hashKeyImpl(): Int = key.hashCode()
