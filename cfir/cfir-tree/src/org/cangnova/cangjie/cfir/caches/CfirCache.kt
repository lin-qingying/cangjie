/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.cfir.caches

import kotlin.reflect.KProperty

@RequiresOptIn("This API exposes CFIR cache internals. It should not used in production.")
annotation class CfirCacheInternals

/**
 * 带内建值计算策略的 CFIR 缓存抽象。
 *
 * 缓存使用 [K] 和 [CONTEXT] 共同参与值 [V] 的计算，
 * 但缓存命中只依赖 key，不区分 context。
 */
abstract class CfirCache<in K : Any, out V, in CONTEXT> {
    abstract fun getValue(key: K, context: CONTEXT): V
    abstract fun getValueIfComputed(key: K): V?

    /**
     * 返回当前缓存中所有非空值的快照。
     */
    @CfirCacheInternals
    abstract val cachedValues: Collection<V>
}

@Suppress("NOTHING_TO_INLINE")
inline fun <K : Any, V> CfirCache<K, V, Nothing?>.getValue(key: K): V =
    getValue(key, null)

operator fun <K : Any, V> CfirCache<K, V, Nothing>.contains(key: K): Boolean {
    return getValueIfComputed(key) != null
}

abstract class CfirLazyValue<out V> {
    abstract fun getValue(): V
}

operator fun <V> CfirLazyValue<V>.getValue(thisRef: Any?, property: KProperty<*>): V {
    return getValue()
}
