package org.cangnova.cangjie.chir.core.analysis

import org.cangnova.cangjie.chir.core.pipeline.ChirAnalysisCache
import org.cangnova.cangjie.chir.core.pipeline.ChirAnalysisDescriptor

/**
 * 基于 [ChirAnalysisCache] 的分析结果提供器。
 */
class ChirAnalysisResultProvider(
    /**
     * 分析结果缓存。
     */
    private val cache: ChirAnalysisCache,
) {
    /**
     * 获取缓存分析结果，不存在时执行 [compute] 并写入缓存。
     */
    fun <T : Any> getOrCompute(
        descriptor: ChirAnalysisDescriptor<T>,
        compute: () -> T,
    ): T {
        val cached = cache.get(descriptor)
        if (cached != null) return cached
        val computed = compute()
        cache.put(descriptor, computed)
        return computed
    }
}
