/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.statistics

import com.github.benmanes.caffeine.cache.RemovalCause
import com.github.benmanes.caffeine.cache.stats.CacheStats
import com.github.benmanes.caffeine.cache.stats.StatsCounter
import io.opentelemetry.api.metrics.Meter

/**
 * A Caffeine [StatsCounter] which delegates to OpenTelemetry counters.
 */
internal class LLCaffeineStatsCounter(meter: Meter, scope: LLCaffeineStatisticsScope) : StatsCounter {
    /**
     * cache hit 次数计数器。
     */
    private val hitCounter = meter.counterBuilder(scope.hits.name).build()

    /**
     * cache miss 次数计数器。
     */
    private val missCounter = meter.counterBuilder(scope.misses.name).build()

    /**
     * cache eviction 次数计数器。
     */
    private val evictionCounter = meter.counterBuilder(scope.evictions.name).build()

    /**
     * 记录 [count] 次 cache hit。
     */
    override fun recordHits(count: Int) {
        hitCounter.add(count.toLong())
    }

    /**
     * 记录 [count] 次 cache miss。
     */
    override fun recordMisses(count: Int) {
        missCounter.add(count.toLong())
    }

    /**
     * OpenTelemetry 路径当前不记录加载成功耗时。
     */
    override fun recordLoadSuccess(loadTime: Long) {
    }

    /**
     * OpenTelemetry 路径当前不记录加载失败耗时。
     */
    override fun recordLoadFailure(loadTime: Long) {
    }

    /**
     * 记录一次 cache eviction。
     */
    override fun recordEviction(weight: Int, cause: RemovalCause) {
        evictionCounter.add(1)
    }

    /**
     * We cannot retrieve any stats from OpenTelemetry, so the snapshot will be empty.
     */
    override fun snapshot(): CacheStats = CacheStats.empty()
}
