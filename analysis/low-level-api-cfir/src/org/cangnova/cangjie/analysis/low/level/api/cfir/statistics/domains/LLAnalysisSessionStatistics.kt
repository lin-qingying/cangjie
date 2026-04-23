/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.domains

import com.github.benmanes.caffeine.cache.stats.StatsCounter
import io.opentelemetry.api.metrics.LongCounter
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLCaffeineStatsCounter
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsScopes
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsService
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.getMeter

/**
 * Statistics for analysis sessions and `analyze` calls.
 */
class LLAnalysisSessionStatistics(statisticsService: LLStatisticsService) : LLStatisticsDomain {
    private val meter = statisticsService.openTelemetry.getMeter(LLStatisticsScopes.AnalysisSessions)
    val resolveCallCacheStatsCounter: StatsCounter =
        LLCaffeineStatsCounter(meter, LLStatisticsScopes.AnalysisSessions.Caches.ResolveCallCache)

    val analyzeCallCounter: LongCounter = meter.counterBuilder(LLStatisticsScopes.AnalysisSessions.Analyze.Invocations.name).build()
    val resolveSymbolCacheStatsCounter: StatsCounter =
        LLCaffeineStatsCounter(meter, LLStatisticsScopes.AnalysisSessions.Caches.ResolveSymbolCache)
    val resolveToSymbolsCacheStatsCounter: StatsCounter =
        LLCaffeineStatsCounter(meter, LLStatisticsScopes.AnalysisSessions.Caches.ResolveToSymbolsCache)

    val lowMemoryCacheCleanupInvocationCounter: LongCounter =
        meter.counterBuilder(LLStatisticsScopes.AnalysisSessions.LowMemoryCacheCleanup.Invocations.name).build()
}
