/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.domains

import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLCaffeineStatsCounter
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsScopes
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsService
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.getMeter

internal class LLSymbolProviderStatistics(statisticsService: LLStatisticsService) : LLStatisticsDomain {
    private val meter = statisticsService.openTelemetry.getMeter(LLStatisticsScopes.SymbolProviders)

    /**
     * A global [Caffeine stats counter][com.github.benmanes.caffeine.cache.stats.StatsCounter] for combined symbol provider *class* caches.
     */
    val combinedSymbolProviderClassCacheStatsCounter =
        LLCaffeineStatsCounter(meter, LLStatisticsScopes.SymbolProviders.Combined.Classes)

    /**
     * A global [Caffeine stats counter][com.github.benmanes.caffeine.cache.stats.StatsCounter] for combined symbol provider *callable*
     * caches.
     */
    val combinedSymbolProviderCallableCacheStatsCounter =
        LLCaffeineStatsCounter(meter, LLStatisticsScopes.SymbolProviders.Combined.Callables)
}
