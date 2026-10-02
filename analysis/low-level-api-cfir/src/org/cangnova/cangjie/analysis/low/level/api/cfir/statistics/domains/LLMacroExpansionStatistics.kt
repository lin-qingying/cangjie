package org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.domains

import io.opentelemetry.api.metrics.LongCounter
import io.opentelemetry.api.metrics.LongHistogram
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.DURATION_BUCKETS_MS
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsScopes
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsService
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.NANOS_PER_MILLI
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.getMeter
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirMacroExpansionOutcome
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirMacroExpansionTimingObserver
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionService

/**
 * macro construction（`MacroConstructionService.expand`）耗时统计域。
 *
 * 每次 construction 记录一次耗时（毫秒直方图），并按结果归类计数
 * （success / degraded / failed / executorUnavailable / blocked），同时累计 construction
 * 次数、覆盖的文件数与宏 surface 数。降级次数直接对应 IDE 卡顿投诉里"宏展开没跑通但
 * 也没有报错"的场景，因此单独可见。
 *
 * 与 Kotlin 对照：Kotlin 的 `LLStatisticsService` 不统计宏展开（K2 没有同名阶段）；
 * 这是仓颉为定位 IDE 卡顿补出的域。
 */
class LLMacroExpansionStatistics(statisticsService: LLStatisticsService) : LLStatisticsDomain, CfirMacroExpansionTimingObserver {
    /**
     * 宏展开的 meter 名字。
     */
    private val meter = statisticsService.openTelemetry.getMeter(LLStatisticsScopes.Macro)

    /**
     * construction 耗时直方图（毫秒）。
     */
    private val duration: LongHistogram = meter.histogramBuilder(LLStatisticsScopes.Macro.Expand.duration())
        .setDescription("Macro construction duration")
        .setUnit("ms")
        .ofLongs()
        .setExplicitBucketBoundariesAdvice(DURATION_BUCKETS_MS)
        .build()

    /**
     * construction 次数。
     */
    private val runs: LongCounter = meter.counterBuilder(LLStatisticsScopes.Macro.Expand.runs())
        .setDescription("Number of macro construction invocations")
        .build()

    /**
     * construction 覆盖的 pre-macro 文件数。
     */
    private val files: LongCounter = meter.counterBuilder(LLStatisticsScopes.Macro.Expand.files())
        .setDescription("Number of pre-macro files covered by macro construction")
        .build()

    /**
     * construction 覆盖的宏 surface 数。
     */
    private val surfaces: LongCounter = meter.counterBuilder(LLStatisticsScopes.Macro.Expand.surfaces())
        .setDescription("Number of macro surfaces covered by macro construction")
        .build()

    /**
     * 按结果归类的次数。
     */
    private val outcomes: Map<CfirMacroExpansionOutcome, LongCounter> =
        CfirMacroExpansionOutcome.entries.associateWith { outcome ->
            meter.counterBuilder(LLStatisticsScopes.Macro.Expand.outcome(outcome))
                .setDescription("Number of macro construction results: ${outcome.metricSuffix}")
                .build()
        }

    override fun onMacroExpansionFinished(
        mode: MacroConstructionService.Mode,
        outcome: CfirMacroExpansionOutcome,
        fileCount: Int,
        surfaceCount: Int,
        elapsedNanos: Long,
    ) {
        duration.record(elapsedNanos / NANOS_PER_MILLI)
        runs.add(1)
        files.add(fileCount.toLong())
        surfaces.add(surfaceCount.toLong())
        outcomes[outcome]?.add(1)
    }
}
