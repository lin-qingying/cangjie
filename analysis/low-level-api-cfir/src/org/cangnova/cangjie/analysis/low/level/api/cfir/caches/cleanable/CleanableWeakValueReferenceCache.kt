/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.caches.cleanable

import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirInternals
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

/**
 * A [CleanableValueReferenceCache] with a [WeakReference] to values [V].
 *
 * @param getCleaner Returns the [ValueReferenceCleaner] that should be invoked after [V] has been collected or removed from the cache. The
 *  function will be invoked once when the value is added to the cache.
 */
@LLCfirInternals
class CleanableWeakValueReferenceCache<K : Any, V : Any>(
    backingMap: ConcurrentHashMap<K, ReferenceWithCleanup<K, V>> = ConcurrentHashMap(),
    referenceQueue: ReferenceQueue<V> = ReferenceQueue(),
    /**
     * 为每个新加入缓存的 value 创建 cleanup 处理器。
     */
    private val getCleaner: (V) -> ValueReferenceCleaner<V>,
) : CleanableValueReferenceCache<K, V>(backingMap, referenceQueue) {
    /**
     * 创建共享相同 weak-reference 语义的新 cache 副本。
     */
    override fun createCopy(
        mapCopy: ConcurrentHashMap<K, ReferenceWithCleanup<K, V>>,
        queueCopy: ReferenceQueue<V>
    ): CleanableValueReferenceCache<K, V> {
        return CleanableWeakValueReferenceCache(mapCopy, queueCopy, getCleaner)
    }

    /**
     * 为 value 创建带 cleanup 的 weak reference。
     */
    override fun createReference(key: K, value: V, queue: ReferenceQueue<V>): ReferenceWithCleanup<K, V> {
        return WeakReferenceWithCleanup(key, value, getCleaner(value), queue)
    }
}

/**
 * 携带 key 与 cleaner 的 weak reference。
 */
private class WeakReferenceWithCleanup<K, V>(
    /**
     * 该 reference 在 backing map 中对应的 key。
     */
    override val key: K,
    value: V,
    /**
     * value 被移除或回收后执行的 cleanup 处理器。
     */
    override val cleaner: ValueReferenceCleaner<V>,
    referenceQueue: ReferenceQueue<V>,
) : WeakReference<V>(value, referenceQueue), ReferenceWithCleanup<K, V> {
    /**
     * 基于 key 与当前 referent 比较 reference 等价性。
     */
    override fun equals(other: Any?): Boolean = equalsImpl(other)

    /**
     * 使用 key 作为 hash，保证 backing map 中 reference 移除匹配稳定。
     */
    override fun hashCode(): Int = hashKeyImpl()
}
