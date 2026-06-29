package org.cangnova.cangjie.analysis.api.platform.caches

import com.github.benmanes.caffeine.cache.Cache
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import java.util.concurrent.ConcurrentMap

/**
 * 不能存 `null` 的缓存容器里使用的空值哨兵。
 */
@CaImplementationDetail
object NullValue

/**
 * 将缓存中保存的 [NullValue] 哨兵还原为 Kotlin null。
 */
@CaImplementationDetail
@Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
inline fun <V> Any.nullValueToNull(): V = when (this) {
    NullValue -> null
    else -> this
} as V

/**
 * 在 [ConcurrentMap] 中缓存允许为 null 的计算结果。
 */
@CaImplementationDetail
inline fun <K : Any, R> ConcurrentMap<K, Any>.getOrPutWithNullableValue(
    key: K,
    crossinline compute: (K) -> Any?,
): R {
    val value = getOrPut(key) { compute(key) ?: NullValue }
    return value.nullValueToNull()
}

/**
 * 在 Caffeine [Cache] 中缓存允许为 null 的计算结果。
 */
@CaImplementationDetail
inline fun <K : Any, R> Cache<K, Any>.getOrPutWithNullableValue(
    key: K,
    crossinline compute: (K) -> Any?,
): R = asMap().getOrPutWithNullableValue(key) { compute(key) }
