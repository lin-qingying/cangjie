

package org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.combined

import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider

/**
 * A symbol provider which combines multiple individual symbol providers of the same type. Combined symbol providers typically have an
 * advantage over naively querying the list of individual symbol providers, such as caching or a single index access with a combined scope.
 */
internal abstract class LLCombinedSymbolProvider<P : CfirSymbolProvider>(session: CfirSession) : CfirSymbolProvider(session) {
    /**
     * 当前 combined provider 聚合的底层 provider 列表。
     */
    abstract val providers: List<P>

    /**
     * Estimates the number of symbols contained in the combined symbol provider's own caches. The metric does not include the cache sizes
     * of the individual symbol providers.
     *
     * The purpose of this metric is to estimate the current **cache overhead** of a combined symbol provider.
     */
    abstract fun estimateSymbolCacheSize(): Long
}
