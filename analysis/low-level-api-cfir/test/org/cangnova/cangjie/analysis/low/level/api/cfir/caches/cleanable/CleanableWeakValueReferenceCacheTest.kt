package org.cangnova.cangjie.analysis.low.level.api.cfir.caches.cleanable

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout

/**
 * `CleanableWeakValueReferenceCache` 的行为单元测试。
 *
 * 覆盖：
 * - put/get/remove 的基本读写与移除时清理；
 * - 替换/删除时的 cleanup 契约（同实例替换不清理）；
 * - computeIfAbsent / compute 的计算次数与清理语义；
 * - createCopy 副本持有独立引用；
 * - value 被 GC 回收后，随后的变更操作触发 null 清理。
 *
 * GC 用例依赖真实垃圾回收，使用多轮 `System.gc()` 重试以降低环境抖动影响。
 */
class CleanableWeakValueReferenceCacheTest {
    /**
     * 创建带清理记录的缓存；每个 value 的 cleanup 参数按序追加到 [cleanups]。
     */
    private fun newCache(cleanups: MutableList<String?>): CleanableWeakValueReferenceCache<String, String> {
        return CleanableWeakValueReferenceCache { _ ->
            ValueReferenceCleaner { cleaned -> cleanups.add(cleaned) }
        }
    }

    /**
     * 验证 put/get 基本读写，remove 返回旧值、清空条目并触发一次带旧值的 cleanup。
     */
    @Test
    fun putGetAndRemoveWithCleanup() {
        val cleanups = mutableListOf<String?>()
        val cache = newCache(cleanups)

        cache.put("k", "v")
        assertEquals("v", cache.get("k"))
        assertEquals(1, cache.size)

        val removed = cache.remove("k")
        assertEquals("v", removed)
        assertNull(cache.get("k"))
        assertEquals(0, cache.size)
        assertEquals(listOf<String?>("v"), cleanups)
    }

    /**
     * 验证 put 替换已有条目时，旧值被清理、新值保留。
     */
    @Test
    fun putReplacementCleansUpReplacedValue() {
        val cleanups = mutableListOf<String?>()
        val cache = newCache(cleanups)

        cache.put("k", "first")
        cache.put("k", "second")

        assertEquals("second", cache.get("k"))
        assertEquals(listOf<String?>("first"), cleanups)
    }

    /**
     * 验证同一实例重复 put 视为未移除，不触发 cleanup。
     */
    @Test
    fun putSameValueInstanceDoesNotCleanup() {
        val cleanups = mutableListOf<String?>()
        val cache = newCache(cleanups)

        val value = "same"
        cache.put("k", value)
        cache.put("k", value)

        assertTrue(cleanups.isEmpty())
        assertEquals("same", cache.get("k"))
    }

    /**
     * 验证 computeIfAbsent 只计算一次，后续读取命中缓存。
     */
    @Test
    fun computeIfAbsentComputesOnlyOnce() {
        val cache = newCache(mutableListOf())
        var computations = 0

        val first = cache.computeIfAbsent("k") {
            computations++
            "computed"
        }
        val second = cache.computeIfAbsent("k") { error("must not recompute for present key") }

        assertSame(first, second)
        assertEquals(1, computations)
        assertEquals("computed", cache.get("k"))
    }

    /**
     * 验证 compute 返回 null 时移除条目并清理旧值。
     */
    @Test
    fun computeReturningNullRemovesEntryAndCleansUp() {
        val cleanups = mutableListOf<String?>()
        val cache = newCache(cleanups)
        cache.put("k", "old")

        val result = cache.compute("k") { _, _ -> null }

        assertNull(result)
        assertNull(cache.get("k"))
        assertEquals(0, cache.size)
        assertEquals(listOf<String?>("old"), cleanups)
    }

    /**
     * 验证 compute 返回同一实例时不创建新引用、不触发清理。
     */
    @Test
    fun computeReturningSameInstanceAvoidsCleanup() {
        val cleanups = mutableListOf<String?>()
        val cache = newCache(cleanups)
        val value = "shared"
        cache.put("k", value)

        val result = cache.compute("k") { _, _ -> value }

        assertSame(value, result)
        assertTrue(cleanups.isEmpty())
        assertEquals("shared", cache.get("k"))
    }

    /**
     * 验证 compute 替换旧值时清理旧值、保留新值。
     */
    @Test
    fun computeReplacementCleansUpReplacedValue() {
        val cleanups = mutableListOf<String?>()
        val cache = newCache(cleanups)
        cache.put("k", "old")

        val newValue = cache.compute("k") { _, oldValue ->
            assertEquals("old", oldValue)
            "new"
        }

        assertEquals("new", newValue)
        assertEquals("new", cache.get("k"))
        assertEquals(listOf<String?>("old"), cleanups)
    }

    /**
     * 验证 clear 清理全部条目并对每个值触发 cleanup。
     */
    @Test
    fun clearCleansUpAllValues() {
        val cleanups = mutableListOf<String?>()
        val cache = newCache(cleanups)
        cache.put("k1", "v1")
        cache.put("k2", "v2")
        cache.put("k3", "v3")

        cache.clear()

        assertTrue(cache.isEmpty())
        assertEquals(0, cache.size)
        assertEquals(listOf<String?>("v1", "v2", "v3"), cleanups)
    }

    /**
     * 验证 createCopy 携带全部存活条目，且副本引用独立于原 cache 的后续移除。
     */
    @Test
    fun createCopyCarriesLiveEntriesWithoutCleaningOriginals() {
        val cleanups = mutableListOf<String?>()
        val cache = newCache(cleanups)
        cache.put("k1", "v1")
        cache.put("k2", "v2")

        val copy = cache.createCopy()
        assertEquals(2, copy.size)
        assertEquals("v1", copy.get("k1"))

        cache.remove("k1")
        assertEquals(listOf<String?>("v1"), cleanups)

        // 副本持有独立 weak reference，原 cache 的移除不影响副本
        assertEquals("v1", copy.get("k1"))
        assertEquals(2, copy.size)
    }

    /**
     * 验证 value 被 GC 回收后，下一次变更操作（此处读 size）触发以 null 调用的 cleanup 并移除条目。
     */
    @Test
    @Timeout(60)
    fun garbageCollectedValueIsCleanedUpOnNextMutatingOperation() {
        val cleanups = mutableListOf<String?>()
        val cache = newCache(cleanups)

        // 必须运行期构造新实例：字符串字面量驻留 JVM 字符串池，弱引用永远无法回收
        var strongReference: String? = String("collectable".toCharArray())
        cache.put("k", strongReference!!)
        strongReference = null

        // 多轮 GC 重试，直到弱引用被回收并进入 reference queue
        for (_attempt in 1..1000) {
            System.gc()
            System.runFinalization()
            // size 内部先 processQueue，处理已回收的 reference
            if (cache.get("k") == null) break
            Thread.sleep(20)
        }

        assertNull(cache.get("k"), "weak value should be collected after gc retries")
        assertEquals(0, cache.size)
        assertEquals(listOf<String?>(null), cleanups)
    }
}
