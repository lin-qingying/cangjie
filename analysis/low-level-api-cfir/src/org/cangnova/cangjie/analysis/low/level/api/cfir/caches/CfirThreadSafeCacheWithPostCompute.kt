/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.caches

import org.cangnova.cangjie.cfir.caches.CfirCache
import org.cangnova.cangjie.cfir.caches.CfirCacheInternals
import java.util.concurrent.ConcurrentHashMap

/**
 * 支持 value 计算后同步执行 post-compute 回调的线程安全 CFIR cache。
 */
internal class CfirThreadSafeCacheWithPostCompute<K : Any, V, CONTEXT, DATA>(
    /**
     * 计算 value 并返回 post-compute 所需附加数据的函数。
     */
    private val createValue: (K, CONTEXT) -> Pair<V, DATA>,
    /**
     * value 创建完成后执行的后处理回调。
     */
    private val postCompute: (K, V, DATA) -> Unit
) : CfirCache<K, V, CONTEXT>() {
    /**
     * 保存带 post-compute 状态机的缓存条目。
     */
    private val map = ConcurrentHashMap<K, ValueWithPostCompute<K, V, DATA>>()

    /**
     * 取得缓存值；未命中时创建新的 post-compute 状态机。
     */
    override fun getValue(key: K, context: CONTEXT): V =
        map.getOrPut(key) {
            ValueWithPostCompute(
                key,
                calculate = { createValue(it, context) },
                postCompute = postCompute
            )
        }.getValue()

    /**
     * 在值已经完整计算并完成 post-compute 后返回缓存值。
     */
    override fun getValueIfComputed(key: K): V? =
        map[key]?.getValueIfComputed()

    /**
     * 返回所有已经完成 post-compute 的缓存值。
     */
    @CfirCacheInternals
    override val cachedValues: Collection<V>
        get() = map.values.mapNotNull { it.getValueIfComputed() }
}
