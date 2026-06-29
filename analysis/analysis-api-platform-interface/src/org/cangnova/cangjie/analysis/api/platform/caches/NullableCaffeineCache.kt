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
    /**
     * 底层 Caffeine cache，内部使用 [NullValue] 哨兵保存可空结果。
     */
    val cache: Cache<K, Any>,
) {
    constructor(configure: (Caffeine<Any, Any>) -> Caffeine<Any, Any>) : this(configure(Caffeine.newBuilder()).build())

    /**
     * 获取 [key] 对应值，缺失时通过 [compute] 计算并允许缓存 null。
     */
    @OptIn(CaImplementationDetail::class)
    inline fun get(key: K, crossinline compute: (K) -> V?): V? =
        cache.get(key) { compute(it) ?: NullValue }?.nullValueToNull()

    /**
     * 获取或写入 [key] 对应的可空值。
     */
    @OptIn(CaImplementationDetail::class)
    inline fun getOrPut(key: K, crossinline compute: (K) -> V?): V? =
        cache.getOrPutWithNullableValue(key) { compute(key) }

    /**
     * 当前 Caffeine cache 的估算条目数量。
     */
    val estimatedSize: @NonNegative Long
        get() = cache.estimatedSize()
}
