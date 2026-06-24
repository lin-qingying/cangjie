/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.cfir.caches

import kotlin.reflect.KProperty

/**
 * 标记暴露 CFIR 缓存内部状态的 API。
 *
 * 使用该注解的成员只应出现在调试、测试或框架诊断代码中，生产解析逻辑不应依赖缓存快照。
 */
@RequiresOptIn("This API exposes CFIR cache internals. It should not used in production.")
annotation class CfirCacheInternals

/**
 * 带内建值计算策略的 CFIR 缓存抽象。
 *
 * 缓存使用 [K] 和 [CONTEXT] 共同参与值 [V] 的计算，
 * 但缓存命中只依赖 key，不区分 context。
 */
abstract class CfirCache<in K : Any, out V, in CONTEXT> {
    /**
     * 读取 [key] 对应的值；缓存缺失时使用 [context] 参与计算。
     */
    abstract fun getValue(key: K, context: CONTEXT): V

    /**
     * 只在 [key] 已经计算过时返回缓存值。
     */
    abstract fun getValueIfComputed(key: K): V?

    /**
     * 返回当前缓存中所有非空值的快照。
     */
    @CfirCacheInternals
    abstract val cachedValues: Collection<V>
}

/**
 * 读取不需要上下文的缓存值。
 */
@Suppress("NOTHING_TO_INLINE")
inline fun <K : Any, V> CfirCache<K, V, Nothing?>.getValue(key: K): V =
    getValue(key, null)

/**
 * 判断不需要上下文的缓存是否已经计算过 [key]。
 */
operator fun <K : Any, V> CfirCache<K, V, Nothing>.contains(key: K): Boolean {
    return getValueIfComputed(key) != null
}

/**
 * 可由缓存工厂创建的惰性值抽象。
 */
abstract class CfirLazyValue<out V> {
    /**
     * 读取并按实现策略初始化惰性值。
     */
    abstract fun getValue(): V
}

/**
 * 让 [CfirLazyValue] 可以作为 Kotlin 委托属性使用。
 */
operator fun <V> CfirLazyValue<V>.getValue(thisRef: Any?, property: KProperty<*>): V {
    return getValue()
}
