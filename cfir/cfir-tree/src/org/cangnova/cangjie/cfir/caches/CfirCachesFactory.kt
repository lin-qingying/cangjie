/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.cfir.caches

import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import kotlin.time.Duration

abstract class CfirCachesFactory : CfirSessionComponent {
    /**
     * 创建按需计算并缓存值的缓存。
     */
    abstract fun <K : Any, V, CONTEXT> createCache(createValue: (K, CONTEXT) -> V): CfirCache<K, V, CONTEXT>

    /**
     * 创建具备初始容量和负载因子的缓存。
     */
    abstract fun <K : Any, V, CONTEXT> createCache(
        initialCapacity: Int,
        loadFactor: Float,
        createValue: (K, CONTEXT) -> V,
    ): CfirCache<K, V, CONTEXT>

    /**
     * 两阶段构建缓存值：先创建并写入缓存，再执行 post-compute。
     */
    abstract fun <K : Any, V, CONTEXT, DATA> createCacheWithPostCompute(
        createValue: (K, CONTEXT) -> Pair<V, DATA>,
        postCompute: (K, V, DATA) -> Unit,
    ): CfirCache<K, V, CONTEXT>

    enum class KeyReferenceStrength {
        STRONG,
        WEAK,
    }

    enum class ValueReferenceStrength {
        STRONG,
        SOFT,
        WEAK,
    }

    /**
     * 创建带建议性限制的缓存实现。
     */
    abstract fun <K : Any, V, CONTEXT> createCacheWithSuggestedLimits(
        expirationAfterAccess: Duration? = null,
        maximumSize: Long? = null,
        keyStrength: KeyReferenceStrength = KeyReferenceStrength.STRONG,
        valueStrength: ValueReferenceStrength = ValueReferenceStrength.STRONG,
        createValue: (K, CONTEXT) -> V,
    ): CfirCache<K, V, CONTEXT>

    abstract fun <V> createLazyValue(createValue: () -> V): CfirLazyValue<V>

    /**
     * 创建可能以软引用持有值的惰性值。
     */
    abstract fun <V> createPossiblySoftLazyValue(createValue: () -> V): CfirLazyValue<V>
}

val CfirSession.cfirCachesFactory: CfirCachesFactory by CfirSession.sessionComponentAccessor()

inline fun <K : Any, V> CfirCachesFactory.createCache(
    crossinline createValue: (K) -> V,
): CfirCache<K, V, Nothing?> = createCache(
    createValue = { key, _ -> createValue(key) },
)

inline fun <K : Any, V> CfirCachesFactory.createCacheWithSuggestedLimits(
    expirationAfterAccess: Duration? = null,
    maximumSize: Long? = null,
    keyHardness: CfirCachesFactory.KeyReferenceStrength = CfirCachesFactory.KeyReferenceStrength.STRONG,
    valueHardness: CfirCachesFactory.ValueReferenceStrength = CfirCachesFactory.ValueReferenceStrength.STRONG,
    crossinline createValue: (K) -> V,
): CfirCache<K, V, Nothing?> =
    createCacheWithSuggestedLimits(expirationAfterAccess, maximumSize, keyHardness, valueHardness) { key, _ -> createValue(key) }
