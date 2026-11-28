package org.cangnova.cangjie.analysis.low.level.api.cfir.caches

import com.github.benmanes.caffeine.cache.Caffeine
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * [CfirCaffeineCache] 的「是否缓存否定结果」契约测试。
 *
 * 该实现由 [CfirThreadSafeCachesFactory.createCacheWithSuggestedLimits] 在需要容量/过期限制时返回。
 * 它必须与 [CfirThreadSafeCache] 遵守同一条契约：`getValueCachingIf` 让调用方拒绝写入空结果，
 * `getValue` 保持「连 null 一起缓存」的既有语义。
 *
 * 这条测试存在的理由：`CfirCache.getValueCachingIf` 曾经是带默认实现的 open 方法，
 * 而 Caffeine 分支没有覆盖它——一旦某个缓存改走该工厂，「索引未就绪」会被静默冻结成
 * 永久缺失，且没有任何编译期或运行期信号。
 */
class CfirCaffeineCacheTest {

    /**
     * 空结果在调用方拒绝缓存时不得写入：两次查询都要重新计算。
     */
    @Test
    fun `negative result is not persisted when caller rejects it`() {
        var computeCount = 0
        val cache = CfirCaffeineCache<Int, String?, Int>(
            cache = Caffeine.newBuilder().maximumSize(16L).build<Int, Any>(),
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
        val cache = CfirCaffeineCache<Int, String?, Int>(
            cache = Caffeine.newBuilder().maximumSize(16L).build<Int, Any>(),
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
     * 不携带策略的 `getValue` 仍缓存 null：既有契约不变。
     */
    @Test
    fun `getValue keeps caching null as before`() {
        var computeCount = 0
        val cache = CfirCaffeineCache<Int, String?, Int>(
            cache = Caffeine.newBuilder().maximumSize(16L).build<Int, Any>(),
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
