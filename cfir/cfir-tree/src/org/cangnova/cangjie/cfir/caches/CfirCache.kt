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
     * 读取 [key] 对应的值；缺失时计算，但只按 [shouldCache] 决定是否写入缓存。
     *
     * 用于「结果可能暂时为空、而这个空结果不应被持久化」的场景：典型例子是按 [K]
     * 查询本模块尚未建好索引的符号——缓存了 null 就等于把「索引还没就绪」冻结成
     * 「这个符号永远不存在」，而会话通常不会因此重建。
     *
     * 这是**必须由实现显式给出语义的抽象方法**，不提供「忽略 [shouldCache]」的默认实现：
     * 默认实现会让「换一个缓存工厂」静默退化成「缓存否定结果」，而这种违约没有任何
     * 编译期或运行期信号，只会在索引未就绪时表现为「符号永久缺失」。
     *
     * 与 Kotlin 的偏离（显式记录）：Kotlin `FirCache` **没有**对应 API，它照常缓存 null（`NullValue` 哨兵），
     * 靠 session 失效（`LLFirSessionInvalidationService` + PSI/ProjectRoot modification tracker）
     * 让否定结果自然过期。仓颉 session 只在模块修改事件时重建，不因 dumb 模式结束而重建，
     * 单靠失效会把「索引还没就绪」冻结到会话结束，因此额外提供按次裁决是否写回的入口。
     */
    abstract fun getValueCachingIf(key: K, context: CONTEXT, shouldCache: (Any?) -> Boolean): V

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
