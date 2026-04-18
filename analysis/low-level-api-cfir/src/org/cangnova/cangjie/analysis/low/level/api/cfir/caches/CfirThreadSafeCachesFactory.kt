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

internal class CfirThreadSafeCachesFactory(private val project: Project) : CfirCachesFactory() {
    override fun <KEY : Any, VALUE, CONTEXT> createCache(createValue: (KEY, CONTEXT) -> VALUE): CfirCache<KEY, VALUE, CONTEXT> =
        CfirThreadSafeCache(createValue = createValue)

    override fun <K : Any, V, CONTEXT> createCache(
        initialCapacity: Int,
        loadFactor: Float,
        createValue: (K, CONTEXT) -> V
    ): CfirCache<K, V, CONTEXT> =
        CfirThreadSafeCache(
            ConcurrentHashMap<K, Any>(initialCapacity, loadFactor),
            createValue
        )


    override fun <KEY : Any, VALUE, CONTEXT, DATA> createCacheWithPostCompute(
        createValue: (KEY, CONTEXT) -> Pair<VALUE, DATA>,
        postCompute: (KEY, VALUE, DATA) -> Unit
    ): CfirCache<KEY, VALUE, CONTEXT> =
        CfirThreadSafeCacheWithPostCompute(createValue, postCompute)

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

    override fun <V> createLazyValue(createValue: () -> V): CfirLazyValue<V> =
        CfirThreadSafeValue(createValue)

    override fun <V> createPossiblySoftLazyValue(createValue: () -> V): CfirLazyValue<V> =
        LLCfirSoftLazyValue(project, createValue)
}

private class CfirCaffeineCache<K : Any, V, CONTEXT>(
    private val cache: Cache<K, Any>,
    private val createValue: (K, CONTEXT) -> V,
) : CfirCache<K, V, CONTEXT>() {

    /**
     * [Cache.get] cannot be used here as [createValue] may access the map recursively.
     */
    override fun getValue(key: K, context: CONTEXT): V = cache.getOrPutWithNullableValue(key) {
        createValue(it, context)
    }

    override fun getValueIfComputed(key: K): V? = cache.getIfPresent(key)?.nullValueToNull()

    @CfirCacheInternals
    override val cachedValues: Collection<V>
        get() = cache.asMap().values.mapNotNull { it.nullValueToNull() }
}
