package org.cangnova.cangjie.analysis.low.level.api.cfir.statistics

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
}
