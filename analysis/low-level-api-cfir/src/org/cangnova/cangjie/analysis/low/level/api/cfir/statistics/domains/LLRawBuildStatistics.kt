package org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.domains

import io.opentelemetry.api.metrics.LongCounter
import io.opentelemetry.api.metrics.LongHistogram
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.DURATION_BUCKETS_MS
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsScopes
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsService
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.NANOS_PER_MILLI
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.getMeter
import org.cangnova.cangjie.cfir.builder.BodyBuildingMode
import org.cangnova.cangjie.cfir.builder.CfirRawBuildSource
import org.cangnova.cangjie.cfir.builder.CfirRawBuildTimingObserver

/**
 * raw CFIR 构建耗时统计域。
 *
 * 数据来源是两条文件级 raw 构建入口：`PsiRawCfirBuilder.buildCfirFile` 与
 * `LightTreeRawCfirDeclarationBuilder.buildCfirFile`。IDE 首开文件走哪条路径由
 * LightTree 可用性决定，两条路径的耗时特征不同，因此按来源分开记录耗时（毫秒直方图）
 * 与构建次数。body 构建策略（立即 / 延迟）只进 description，不拆指标名，避免指标爆炸。
 *
 * 与 Kotlin 对照：Kotlin 的 `LLStatisticsService` 不统计 raw FIR 构建；
 * 这是仓颉为定位 IDE 首开卡顿补出的域。
 */
class LLRawBuildStatistics(statisticsService: LLStatisticsService) : LLStatisticsDomain, CfirRawBuildTimingObserver {
    /**
     * raw 构建耗时的 meter 名字。
     */
    private val meter = statisticsService.openTelemetry.getMeter(LLStatisticsScopes.RawBuild)

    /**
     * 按来源的构建耗时直方图（毫秒）。
     */
    private val durations: Map<CfirRawBuildSource, LongHistogram> =
        CfirRawBuildSource.entries.associateWith { source ->
            meter.histogramBuilder(LLStatisticsScopes.RawBuild.duration(source))
                .setDescription("Raw CFIR build duration from ${source.metricSuffix}")
                .setUnit("ms")
                .ofLongs()
                .setExplicitBucketBoundariesAdvice(DURATION_BUCKETS_MS)
                .build()
        }

    /**
     * 按来源的构建次数。
     */
    private val runs: Map<CfirRawBuildSource, LongCounter> =
        CfirRawBuildSource.entries.associateWith { source ->
            meter.counterBuilder(LLStatisticsScopes.RawBuild.runs(source))
                .setDescription("Number of raw CFIR file builds from ${source.metricSuffix}")
                .build()
        }

    override fun onRawBuildFinished(source: CfirRawBuildSource, bodyBuildingMode: BodyBuildingMode, elapsedNanos: Long) {
        durations[source]?.record(elapsedNanos / NANOS_PER_MILLI)
        runs[source]?.add(1)
    }
}
