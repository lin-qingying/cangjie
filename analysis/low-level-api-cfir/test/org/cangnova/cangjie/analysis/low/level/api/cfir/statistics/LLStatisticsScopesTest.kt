package org.cangnova.cangjie.analysis.low.level.api.cfir.statistics

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsMetricNames
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.junit.jupiter.api.Test

/**
 * [LLStatisticsScopes] 的指标命名空间测试。
 *
 * 指标最终由平台导出并按名字聚合，根 scope 必须落在仓颉自己的命名空间下，
 * 否则会与同进程内的 Kotlin Analysis API 指标混在一起。
 */
class LLStatisticsScopesTest {
    /**
     * 根 scope 必须是 `cangjie.analysis`。
     */
    @Test
    fun rootScopeUsesCangjieNamespace() {
        assertEquals("cangjie.analysis", LLStatisticsScopes.name)
    }

    /**
     * 所有派生 scope 都必须以根 scope 为前缀。
     */
    @Test
    fun derivedScopesArePrefixedWithRoot() {
        val derivedScopes = listOf(
            LLStatisticsScopes.AnalysisSessions,
            LLStatisticsScopes.AnalysisSessions.Analyze,
            LLStatisticsScopes.AnalysisSessions.Analyze.Invocations,
            LLStatisticsScopes.AnalysisSessions.LowMemoryCacheCleanup,
            LLStatisticsScopes.AnalysisSessions.LowMemoryCacheCleanup.Invocations,
            LLStatisticsScopes.AnalysisSessions.Caches.ResolveCallCache,
            LLStatisticsScopes.AnalysisSessions.Caches.ResolveCallCache.Hits,
            LLStatisticsScopes.AnalysisSessions.Caches.ResolveCallCache.Misses,
            LLStatisticsScopes.AnalysisSessions.Caches.ResolveCallCache.Evictions,
            LLStatisticsScopes.AnalysisSessions.Caches.ResolveSymbolCache,
            LLStatisticsScopes.AnalysisSessions.Caches.ResolveToSymbolsCache,
            LLStatisticsScopes.Resolve,
            LLStatisticsScopes.Resolve.Phases,
            LLStatisticsScopes.SymbolProviders,
            LLStatisticsScopes.SymbolProviders.Combined,
            LLStatisticsScopes.SymbolProviders.Combined.Classes,
            LLStatisticsScopes.SymbolProviders.Combined.Callables,
        )

        derivedScopes.forEach { scope ->
            assertTrue(
                scope.name.startsWith("${LLStatisticsScopes.name}."),
                "scope ${scope.name} 必须以根 scope ${LLStatisticsScopes.name} 为前缀。",
            )
        }
    }

    /**
     * 公开名称视图必须与 internal scope 同源：任何一边改名，另一边都要跟着改。
     *
     * 跨模块的性能测试模块只能看到 [LLStatisticsMetricNames]，看不到 internal 的
     * [LLStatisticsScopes]；两者一旦漂移，测试会对着不存在的指标做断言而不报错。
     */
    @Test
    @OptIn(LLStatisticsOnlyApi::class)
    fun publicMetricNamesMirrorInternalScopes() {
        assertEquals(LLStatisticsScopes.name, LLStatisticsMetricNames.root)
        assertEquals(LLStatisticsScopes.AnalysisSessions.Analyze.Invocations.name, LLStatisticsMetricNames.analyzeInvocations)
        assertEquals(LLStatisticsScopes.AnalysisSessions.LowMemoryCacheCleanup.Invocations.name, LLStatisticsMetricNames.lowMemoryCacheCleanupInvocations)
        assertEquals(LLStatisticsScopes.AnalysisSessions.Caches.ResolveCallCache.hits.name, LLStatisticsMetricNames.resolveCallCacheHits)
        assertEquals(LLStatisticsScopes.AnalysisSessions.Caches.ResolveCallCache.misses.name, LLStatisticsMetricNames.resolveCallCacheMisses)
        assertEquals(LLStatisticsScopes.AnalysisSessions.Caches.ResolveSymbolCache.hits.name, LLStatisticsMetricNames.resolveSymbolCacheHits)
        assertEquals(LLStatisticsScopes.AnalysisSessions.Caches.ResolveSymbolCache.misses.name, LLStatisticsMetricNames.resolveSymbolCacheMisses)
        assertEquals(LLStatisticsScopes.AnalysisSessions.Caches.ResolveToSymbolsCache.hits.name, LLStatisticsMetricNames.resolveToSymbolsCacheHits)
        assertEquals(LLStatisticsScopes.AnalysisSessions.Caches.ResolveToSymbolsCache.misses.name, LLStatisticsMetricNames.resolveToSymbolsCacheMisses)
        assertEquals(LLStatisticsScopes.SymbolProviders.Combined.Classes.hits.name, LLStatisticsMetricNames.combinedSymbolProviderClassCacheHits)
        assertEquals(LLStatisticsScopes.SymbolProviders.Combined.Classes.misses.name, LLStatisticsMetricNames.combinedSymbolProviderClassCacheMisses)
        assertEquals(LLStatisticsScopes.SymbolProviders.Combined.Callables.hits.name, LLStatisticsMetricNames.combinedSymbolProviderCallableCacheHits)
        assertEquals(LLStatisticsScopes.SymbolProviders.Combined.Callables.misses.name, LLStatisticsMetricNames.combinedSymbolProviderCallableCacheMisses)
        CfirResolvePhase.entries.forEach { phase ->
            assertEquals(LLStatisticsScopes.Resolve.Phases.duration(phase), LLStatisticsMetricNames.resolvePhaseDuration(phase))
            assertEquals(LLStatisticsScopes.Resolve.Phases.files(phase), LLStatisticsMetricNames.resolvePhaseFiles(phase))
            assertEquals(LLStatisticsScopes.Resolve.Phases.runs(phase), LLStatisticsMetricNames.resolvePhaseRuns(phase))
        }
    }

    /**
     * 阶段指标名由阶段名拼出，必须逐个落在根 scope 下，且带上阶段名。
     */
    @Test
    @OptIn(LLStatisticsOnlyApi::class)
    fun phaseMetricNamesArePrefixedWithRootAndCarryPhaseName() {
        CfirResolvePhase.entries.filterNot { it.noProcessor }.forEach { phase ->
            val expectedSuffix = ".resolve.phases.${phase.name.lowercase()}."
            listOf(
                LLStatisticsMetricNames.resolvePhaseDuration(phase),
                LLStatisticsMetricNames.resolvePhaseFiles(phase),
                LLStatisticsMetricNames.resolvePhaseRuns(phase),
            ).forEach { name ->
                assertTrue(name.startsWith("${LLStatisticsScopes.name}$expectedSuffix"), "阶段指标 $name 命名不符")
            }
        }
    }

}
