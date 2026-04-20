package org.cangnova.cangjie.analysis.api.platform.caches

import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * 对位 Kotlin `NullableConcurrentCache` 的可空缓存包装。
 */
@CaPlatformInterface
@JvmInline
value class NullableConcurrentCache<K : Any, V>(
    val map: ConcurrentMap<K, Any> = ConcurrentHashMap(),
) {
    @OptIn(CaImplementationDetail::class)
    inline fun getOrPut(
        key: K,
        crossinline compute: (K) -> V?,
    ): V {
        return map.getOrPutWithNullableValue(key) { compute(key) }
    }
}
