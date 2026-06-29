/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.caches

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.platform.caches.getOrPutWithNullableValue
import org.cangnova.cangjie.analysis.api.platform.caches.nullValueToNull
import org.cangnova.cangjie.cfir.caches.CfirCache
import org.cangnova.cangjie.cfir.caches.CfirCacheInternals
import org.cangnova.cangjie.cfir.caches.CfirCachesFactory
import org.cangnova.cangjie.cfir.caches.CfirLazyValue
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.toJavaDuration

/**
 * low-level CFIR 使用的线程安全 cache 工厂。
 */
internal class CfirThreadSafeCachesFactory(
    /**
     * 创建 soft lazy value 时使用的 IntelliJ project。
     */
    private val project: Project,
) : CfirCachesFactory() {
    /**
     * 创建默认线程安全 cache。
     */
    override fun <KEY : Any, VALUE, CONTEXT> createCache(createValue: (KEY, CONTEXT) -> VALUE): CfirCache<KEY, VALUE, CONTEXT> =
        CfirThreadSafeCache(createValue = createValue)

    /**
     * 创建带初始容量与负载因子的线程安全 cache。
     */
    override fun <K : Any, V, CONTEXT> createCache(
        initialCapacity: Int,
        loadFactor: Float,
        createValue: (K, CONTEXT) -> V
    ): CfirCache<K, V, CONTEXT> =
        CfirThreadSafeCache(
            ConcurrentHashMap<K, Any>(initialCapacity, loadFactor),
            createValue
        )


    /**
     * 创建在 value 计算后执行 post-compute 回调的线程安全 cache。
     */
    override fun <KEY : Any, VALUE, CONTEXT, DATA> createCacheWithPostCompute(
        createValue: (KEY, CONTEXT) -> Pair<VALUE, DATA>,
        postCompute: (KEY, VALUE, DATA) -> Unit
    ): CfirCache<KEY, VALUE, CONTEXT> =
        CfirThreadSafeCacheWithPostCompute(createValue, postCompute)

    /**
     * 根据大小、过期时间和引用强度建议创建普通 cache 或 Caffeine cache。
     */
    override fun <K : Any, V, CONTEXT> createCacheWithSuggestedLimits(
        expirationAfterAccess: Duration?,
        maximumSize: Long?,
        keyStrength: KeyReferenceStrength,
        valueStrength: ValueReferenceStrength,
        createValue: (K, CONTEXT) -> V
    ): CfirCache<K, V, CONTEXT> {
        if (
            expirationAfterAccess == null &&
            maximumSize == null &&
            keyStrength == KeyReferenceStrength.STRONG &&
            valueStrength == ValueReferenceStrength.STRONG
        ) {
            return createCache(createValue)
        }

        val builder = Caffeine<K, V>.newBuilder()

        if (expirationAfterAccess != null) {
            builder.expireAfterAccess(expirationAfterAccess.toJavaDuration())
        }

        if (maximumSize != null) {
            builder.maximumSize(maximumSize)
        }

        if (keyStrength == KeyReferenceStrength.WEAK) {
            builder.weakKeys()
        }

        when (valueStrength) {
            ValueReferenceStrength.STRONG -> {}
            ValueReferenceStrength.SOFT -> builder.softValues()
            ValueReferenceStrength.WEAK -> builder.weakValues()
        }

        return CfirCaffeineCache(builder.build(), createValue)
    }

    /**
     * 创建强引用的同步 lazy value。
     */
    override fun <V> createLazyValue(createValue: () -> V): CfirLazyValue<V> =
        CfirThreadSafeValue(createValue)

    /**
     * 创建可能被 GC 回收的 soft lazy value。
     */
    override fun <V> createPossiblySoftLazyValue(createValue: () -> V): CfirLazyValue<V> =
        LLCfirSoftLazyValue(project, createValue)
}

/**
 * 使用 Caffeine 实现的受限 CFIR cache 包装。
 */
private class CfirCaffeineCache<K : Any, V, CONTEXT>(
    /**
     * 持有实际缓存条目的 Caffeine cache。
     */
    private val cache: Cache<K, Any>,
    /**
     * cache 未命中时创建值的函数。
     */
    private val createValue: (K, CONTEXT) -> V,
) : CfirCache<K, V, CONTEXT>() {

    /**
     * [Cache.get] cannot be used here as [createValue] may access the map recursively.
     */
    override fun getValue(key: K, context: CONTEXT): V = cache.getOrPutWithNullableValue(key) {
        createValue(it, context)
    }

    /**
     * 只返回已经保存在 Caffeine 中的条目，不触发创建。
     */
    override fun getValueIfComputed(key: K): V? = cache.getIfPresent(key)?.nullValueToNull()

    /**
     * 返回当前 Caffeine cache 中所有非空缓存值。
     */
    @CfirCacheInternals
    override val cachedValues: Collection<V>
        get() = cache.asMap().values.mapNotNull { it.nullValueToNull() }
}
