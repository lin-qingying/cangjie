package org.cangnova.cangjie.analysis.low.level.api.cfir.caches

import org.cangnova.cangjie.analysis.api.platform.caches.getOrPutWithNullableValue
import org.cangnova.cangjie.analysis.api.platform.caches.nullValueToNull
import org.cangnova.cangjie.cfir.caches.CfirCache
import org.cangnova.cangjie.cfir.caches.CfirCacheInternals
import java.util.concurrent.ConcurrentHashMap

/**
 * 基于 `ConcurrentHashMap` 的线程安全 CFIR cache，支持缓存 null 值。
 *
 * 该实现只承载 [CfirCache] 的单一契约：**命中只看 key，context 只参与首次计算**。
 * 如果某个 key 的结果依赖于「查询时提供的上下文有多精确」，那不是本缓存能表达的事情——
 * 应当把精度搬进 key（见 `LLPsiAwareClassLikeSymbolCache` 的双缓存设计），
 * 而不是在运行期检测并「修正不一致的值」：后一种做法只能把契约错误变成错误日志。
 */
internal class CfirThreadSafeCache<K : Any, V, CONTEXT>(
    /**
     * 保存缓存条目的并发 map，内部会用哨兵对象表示 null 值。
     */
    private val map: ConcurrentHashMap<K, Any> = ConcurrentHashMap(),
    /**
     * 在 key 未命中时根据 key 与上下文创建缓存值的函数。
     */
    private val createValue: (K, CONTEXT) -> V,
) : CfirCache<K, V, CONTEXT>() {
    /**
     * 取得缓存值；未命中时以线程安全方式调用 [createValue]。
     */
    override fun getValue(key: K, context: CONTEXT): V = map.getOrPutWithNullableValue(key) {
        createValue(it, context)
    }

    /**
     * 取得缓存值；未命中时计算，但**只有当 [shouldCache] 通过时才写回缓存**。
     *
     * 非空值一律允许写回由调用方决定；空值默认不写回（见 [CfirCache.getValueCachingIf]），
     * 因此不存在「把暂时缺失冻结成永久结论」的路径。
     */
    override fun getValueCachingIf(key: K, context: CONTEXT, shouldCache: (Any?) -> Boolean): V {
        map[key]?.let { return it.nullValueToNull() }

        val value = createValue(key, context)
        if (value != null && shouldCache(value)) {
            @Suppress("UNCHECKED_CAST")
            map.putIfAbsent(key, value as Any)
        }
        return value
    }

    /**
     * 只在条目已经计算完成时返回值，不触发新计算。
     */
    override fun getValueIfComputed(key: K): V? = map[key]?.nullValueToNull()

    /**
     * 返回当前所有已缓存的非空值，供 CFIR cache 内部调试和失效逻辑使用。
     */
    @CfirCacheInternals
    override val cachedValues: Collection<V>
        get() = map.values.mapNotNull { it.nullValueToNull() }
}
