package org.cangnova.cangjie.analysis.api.platform.caches

import com.github.benmanes.caffeine.cache.Cache
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
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
@OptIn(CaPlatformInterface::class)
inline fun <K : Any, R> Cache<K, Any>.getOrPutWithNullableValue(
    key: K,
    crossinline compute: (K) -> Any?,
): R {
    // 这里不能用 `asMap().getOrPutWithNullableValue`：那样会绕过 Caffeine 的命中/未命中记录，
    // 接入 StatsCounter 的缓存指标（analysisSessions.caches.*）将恒为 0。
    val value = getOrPut(key) { compute(key) ?: NullValue }
    return value.nullValueToNull()
}
