package org.cangnova.cangjie.analysis.low.level.api.cfir.caches

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * [CfirThreadSafeCache] 的「是否缓存否定结果」契约测试。
 *
 * 这条策略是本轮修复的核心之一：库索引就绪前的一次 miss 若被写进长生命周期缓存，
 * 就会在同一个 session 里退化成永久缺失（session 只在模块修改事件时重建）。
 * 因此 [CfirThreadSafeCache.getValueCachingIf] 必须让调用方有权拒绝写入空结果，
 * 而 [CfirThreadSafeCache.getValue] 保持既有的「缓存 null」语义不被破坏。
 */
class CfirThreadSafeCacheTest {

    /**
     * 空结果在调用方拒绝缓存时不得写入：两次查询都要重新计算。
     */
    @Test
    fun `negative result is not persisted when caller rejects it`() {
        var computeCount = 0
        val cache = CfirThreadSafeCache<Int, String?, Int>(
            createValue = { _, _ ->
                computeCount++
                null
            },
        )

        assertNull(cache.getValueCachingIf(1, context = 0) { it != null })
        assertNull(cache.getValueCachingIf(1, context = 0) { it != null })

        assertEquals(2, computeCount, "未通过 shouldCache 的空结果不应被缓存")
        assertNull(cache.getValueIfComputed(1), "缓存中不应留下任何条目")
    }

    /**
     * 非空结果按常规写入缓存，后续查询直接命中。
     */
    @Test
    fun `non-null result is cached and reused`() {
        var computeCount = 0
        val cache = CfirThreadSafeCache<Int, String?, Int>(
            createValue = { key, _ ->
                computeCount++
                "value-$key"
            },
        )

        assertEquals("value-1", cache.getValueCachingIf(1, context = 0) { it != null })
        assertEquals("value-1", cache.getValueCachingIf(1, context = 0) { it != null })

        assertEquals(1, computeCount)
        assertEquals("value-1", cache.getValueIfComputed(1))
    }

    /**
     * 不携带策略的 `getValue` 仍缓存 null：既有契约不变，改动只发生在显式请求策略的入口。
     */
    @Test
    fun `getValue keeps caching null as before`() {
        var computeCount = 0
        val cache = CfirThreadSafeCache<Int, String?, Int>(
            createValue = { _, _ ->
                computeCount++
                null
            },
        )

        assertNull(cache.getValue(1, context = 0))
        assertNull(cache.getValue(1, context = 0))

        assertEquals(1, computeCount, "getValue 的既有语义是连 null 一起缓存")
    }
}
