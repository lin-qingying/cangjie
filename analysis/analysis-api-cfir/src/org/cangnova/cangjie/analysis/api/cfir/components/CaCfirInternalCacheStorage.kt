package org.cangnova.cangjie.analysis.api.cfir.components

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.intellij.psi.util.CachedValue
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.references.CaCfirReference
import org.cangnova.cangjie.analysis.api.lifetime.CaSessionComponent
import org.cangnova.cangjie.analysis.api.platform.CaCachedService
import org.cangnova.cangjie.analysis.api.platform.caches.NullableCaffeineCache
import org.cangnova.cangjie.analysis.api.platform.caches.NullableConcurrentCache
import org.cangnova.cangjie.analysis.api.platform.caches.withStatsCounter
import org.cangnova.cangjie.analysis.api.resolution.CaCallResolutionAttempt
import org.cangnova.cangjie.analysis.api.resolution.CaSymbolResolutionAttempt
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure.LLCfirInBlockModificationTracker
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsService
import org.cangnova.cangjie.analysis.utils.caches.softCachedValue
import org.cangnova.cangjie.psi.CjElement
import java.util.concurrent.ConcurrentHashMap

/**
 * 这是直接存放在 [CaSession] 内部的专用缓存位置。
 *
 * 对于 [CaSessionComponent]，这类缓存可以直接放在组件使用点附近；
 * 但对于不属于组件的对象则不行。
 * 例如 [CaCfirReference] 不是 session component，因此这里为它提供统一的
 * [CaSession] 生命周期与失效语义，使 [CaCfirReference.resolveToSymbols] 可以安全缓存。
 *
 * 此外，这个存储还提供了 [softCachedValueWithPsiKey] 这类统一入口。
 */

internal class CaCfirInternalCacheStorage(private val analysisSession: CaCfirSession) {
    @CaCachedService
    private val statisticsService by lazy(LazyThreadSafetyMode.PUBLICATION) {
        LLStatisticsService.getInstance(project)
    }

    private val project get() = analysisSession.project

    @OptIn(CaPlatformInterface::class)
    val resolveCallCache: CachedValue<NullableCaffeineCache<CjElement, CaCallResolutionAttempt>> by lazy {
        softCachedValueWithPsiKey {
            NullableCaffeineCache {
                it.withStatsCounter(statisticsService?.analysisSessions?.resolveCallCacheStatsCounter)
            }
        }
    }

    @OptIn(CaPlatformInterface::class)
    val resolveSymbolCache: CachedValue<NullableCaffeineCache<CjElement, CaSymbolResolutionAttempt>> by lazy {
        softCachedValueWithPsiKey {
            NullableCaffeineCache {
                it.withStatsCounter(statisticsService?.analysisSessions?.resolveSymbolCacheStatsCounter)
            }
        }
    }

    @OptIn(CaPlatformInterface::class)
    val resolveToSymbolsCache: CachedValue<Cache<CaCfirReference, Collection<CaSymbol>>> by lazy {
        softCachedValueWithPsiKey {
            Caffeine.newBuilder()
                .withStatsCounter(statisticsService?.analysisSessions?.resolveToSymbolsCacheStatsCounter)
                .build()
        }
    }

    /**
     * The lifetime of this cache is the same as the corresponding [org.jetbrains.kotlin.analysis.api.CaSession],
     * so it doesn't require additional invalidation.
     *
     * The only case where we need to invalidate FIR without the containing session being invalidated is
     * [in-block modification][org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.LLCfirDeclarationModificationService].
     */
    private inline fun <T> softCachedValueWithPsiKey(crossinline createValue: () -> T): CachedValue<T> {
        return softCachedValue(project, LLCfirInBlockModificationTracker.getInstance(project)) {
            createValue()
        }
    }
}
