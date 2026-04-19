package org.cangnova.cangjie.analysis.api.platform.caches

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.checkerframework.checker.index.qual.NonNegative

/**
 * 对位 Kotlin `NullableCaffeineCache` 的可空 Caffeine 包装。
 */
@CaPlatformInterface
@JvmInline
value class NullableCaffeineCache<K : Any, V : Any>(
    val cache: Cache<K, Any>,
) {
    constructor(configure: (Caffeine<Any, Any>) -> Caffeine<Any, Any>) : this(configure(Caffeine.newBuilder()).build())

    @OptIn(CaImplementationDetail::class)
    inline fun get(key: K, crossinline compute: (K) -> V?): V? =
        cache.get(key) { compute(it) ?: NullValue }?.nullValueToNull()

    @OptIn(CaImplementationDetail::class)
    inline fun getOrPut(key: K, crossinline compute: (K) -> V?): V? =
        cache.getOrPutWithNullableValue(key) { compute(key) }

    val estimatedSize: @NonNegative Long
        get() = cache.estimatedSize()
}
