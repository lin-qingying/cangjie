package org.cangnova.cangjie.chir.core.analysis

import org.cangnova.cangjie.chir.core.pipeline.ChirAnalysisCache
import org.cangnova.cangjie.chir.core.pipeline.ChirAnalysisDescriptor

class ChirAnalysisResultProvider(
    private val cache: ChirAnalysisCache,
) {
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
