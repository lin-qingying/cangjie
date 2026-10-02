package org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.domains

import io.opentelemetry.api.metrics.LongCounter
import io.opentelemetry.api.metrics.LongHistogram
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.CaModuleKind
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.DURATION_BUCKETS_MS
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsScopes
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsService
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.NANOS_PER_MILLI
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.getMeter

/**
 * session 创建耗时统计域。
 *
 * 打开工程、切换分支、加载库都会批量创建 session，这是"IDE 打开后卡一会儿"的常见来源。
 * 按 [CaModuleKind] 分桶是因为各类模块的创建成本差一个量级：builtins 与 library 走 stub /
 * cjo 反序列化，source 走 PSI 索引，混在一个计数器里看不出是谁慢。
 *
 * scope session 的创建单独计数（不记耗时）：它只是一个空 `ScopeSession`，耗时没有诊断价值，
 * 但创建次数直接反映 scope 缓存失效频率——PSI 修改后每次都要重建，是失效风暴的信号。
 *
 * 与 Kotlin 对照：Kotlin 的 `LLStatisticsService` 不统计 session 创建；
 * 这是仓颉为定位 IDE 卡顿补出的域。
 */
class LLSessionStatistics(statisticsService: LLStatisticsService) : LLStatisticsDomain {
    /**
     * session 创建耗时的 meter 名字。
     */
    private val meter = statisticsService.openTelemetry.getMeter(LLStatisticsScopes.SessionCreation)

    /**
     * 按模块种类分桶的 session 创建直方图。
     *
     * 不为 [CaModuleKind.UNKNOWN] 建桶：未识别模块不应被算进任何已知种类的耗时。
     */
    private val durations: Map<CaModuleKind, LongHistogram> =
        CaModuleKind.entries.filter { it != CaModuleKind.UNKNOWN }.associateWith { kind ->
            meter.histogramBuilder(LLStatisticsScopes.SessionCreation.duration(kind))
                .setDescription("Session creation duration for ${kind.metricSuffix} modules")
                .setUnit("ms")
                .ofLongs()
                .setExplicitBucketBoundariesAdvice(DURATION_BUCKETS_MS)
                .build()
        }

    /**
     * 按模块种类分桶的 session 创建次数。
     */
    private val runs: Map<CaModuleKind, LongCounter> =
        CaModuleKind.entries.filter { it != CaModuleKind.UNKNOWN }.associateWith { kind ->
            meter.counterBuilder(LLStatisticsScopes.SessionCreation.runs(kind))
                .setDescription("Number of sessions created for ${kind.metricSuffix} modules")
                .build()
        }

    /**
     * scope session 创建次数。
     */
    private val scopeSessions: LongCounter = meter.counterBuilder(LLStatisticsScopes.Scopes.sessionCreated())
        .setDescription("Number of scope sessions created (scope cache misses and invalidations)")
        .build()

    /**
     * 记录一次 session 创建；[CaModuleKind.UNKNOWN] 不上报。
     */
    fun onSessionCreated(kind: CaModuleKind, elapsedNanos: Long) {
        durations[kind]?.record(elapsedNanos / NANOS_PER_MILLI)
        runs[kind]?.add(1)
    }

    /**
     * 记录一次 scope session 创建。
     */
    fun onScopeSessionCreated() = scopeSessions.add(1)
}
