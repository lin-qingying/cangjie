/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.cfir.caches

import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import kotlin.time.Duration

/**
 * CFIR session 级缓存工厂。
 *
 * 不同运行环境可以提供不同缓存策略，例如普通强引用缓存、弱键缓存、软引用值缓存，
 * 上层解析代码只依赖该抽象创建缓存。
 */
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

    /**
     * 缓存键引用强度。
     */
    enum class KeyReferenceStrength {
        /**
         * 使用强引用持有键。
         */
        STRONG,

        /**
         * 使用弱引用持有键。
         */
        WEAK,
    }

    /**
     * 缓存值引用强度。
     */
    enum class ValueReferenceStrength {
        /**
         * 使用强引用持有值。
         */
        STRONG,

        /**
         * 使用软引用持有值。
         */
        SOFT,

        /**
         * 使用弱引用持有值。
         */
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

    /**
     * 创建普通惰性值。
     */
    abstract fun <V> createLazyValue(createValue: () -> V): CfirLazyValue<V>

    /**
     * 创建可能以软引用持有值的惰性值。
     */
    abstract fun <V> createPossiblySoftLazyValue(createValue: () -> V): CfirLazyValue<V>
}

/**
 * 从 session 中读取 CFIR 缓存工厂。
 */
val CfirSession.cfirCachesFactory: CfirCachesFactory by CfirSession.sessionComponentAccessor()

/**
 * 创建不需要上下文参数的缓存。
 */
inline fun <K : Any, V> CfirCachesFactory.createCache(
    crossinline createValue: (K) -> V,
): CfirCache<K, V, Nothing?> = createCache(
    createValue = { key, _ -> createValue(key) },
)

/**
 * 创建不需要上下文参数、但带建议性限制的缓存。
 */
inline fun <K : Any, V> CfirCachesFactory.createCacheWithSuggestedLimits(
    expirationAfterAccess: Duration? = null,
    maximumSize: Long? = null,
    keyHardness: CfirCachesFactory.KeyReferenceStrength = CfirCachesFactory.KeyReferenceStrength.STRONG,
    valueHardness: CfirCachesFactory.ValueReferenceStrength = CfirCachesFactory.ValueReferenceStrength.STRONG,
    crossinline createValue: (K) -> V,
): CfirCache<K, V, Nothing?> =
    createCacheWithSuggestedLimits(expirationAfterAccess, maximumSize, keyHardness, valueHardness) { key, _ -> createValue(key) }
