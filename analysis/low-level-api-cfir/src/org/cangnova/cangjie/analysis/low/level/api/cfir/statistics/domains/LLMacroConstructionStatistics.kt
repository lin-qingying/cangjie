package org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.domains

import io.opentelemetry.api.metrics.LongCounter
import io.opentelemetry.api.metrics.LongHistogram
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.DURATION_BUCKETS_MS
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsScopes
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsService
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.NANOS_PER_MILLI
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.getMeter
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirMacroConstructionStage
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirMacroConstructionTimingObserver
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirMacroExpansionOutcome
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionService

/**
 * macro construction 耗时统计域。
 *
 * construction 主流程分三段（[CfirMacroConstructionStage]），每段各记耗时（毫秒直方图）
 * 与执行次数。展开段额外按结果归类计数（success / degraded / failed /
 * executorUnavailable / blocked），并累计覆盖的 pre-macro 文件数与宏 surface 数。
 *
 * 降级次数单独可见是刻意设计：IDE 卡顿投诉里最隐蔽的一类是"宏展开没跑通但也没报错"，
 * 只看总耗时无法把它与"正常展开但慢"区分开。
 *
 * 与 Kotlin 对照：Kotlin 的 `LLStatisticsService` 不统计宏展开（K2 没有同名阶段）；
 * 这是仓颉为定位 IDE 卡顿补出的域。
 */
class LLMacroConstructionStatistics(statisticsService: LLStatisticsService) :
    LLStatisticsDomain,
    CfirMacroConstructionTimingObserver {

    /**
     * 宏 construction 的 meter 名字。
     */
    private val meter = statisticsService.openTelemetry.getMeter(LLStatisticsScopes.Macro)

    /**
     * 各阶段耗时直方图（毫秒）。
     */
    private val durations: Map<CfirMacroConstructionStage, LongHistogram> =
        CfirMacroConstructionStage.entries.associateWith { stage ->
            meter.histogramBuilder(LLStatisticsScopes.Macro.stage(stage).duration())
                .setDescription("Macro construction duration at ${stage.metricSuffix} stage")
                .setUnit("ms")
                .ofLongs()
                .setExplicitBucketBoundariesAdvice(DURATION_BUCKETS_MS)
                .build()
        }

    /**
     * 各阶段执行次数。
     */
    private val runs: Map<CfirMacroConstructionStage, LongCounter> =
        CfirMacroConstructionStage.entries.associateWith { stage ->
            meter.counterBuilder(LLStatisticsScopes.Macro.stage(stage).runs())
                .setDescription("Number of macro construction ${stage.metricSuffix} stage executions")
                .build()
        }

    /**
     * construction 覆盖的 pre-macro 文件数（仅展开段）。
     */
    private val files: LongCounter = meter.counterBuilder(LLStatisticsScopes.Macro.Expansion.files())
        .setDescription("Number of pre-macro files covered by macro construction")
        .build()

    /**
     * construction 覆盖的宏 surface 数（仅展开段）。
     */
    private val surfaces: LongCounter = meter.counterBuilder(LLStatisticsScopes.Macro.Expansion.surfaces())
        .setDescription("Number of macro surfaces covered by macro construction")
        .build()

    /**
     * 按结果归类的次数（仅展开段）。
     */
    private val outcomes: Map<CfirMacroExpansionOutcome, LongCounter> =
        CfirMacroExpansionOutcome.entries.associateWith { outcome ->
            meter.counterBuilder(LLStatisticsScopes.Macro.Expansion.outcome(outcome))
                .setDescription("Number of macro construction results: ${outcome.metricSuffix}")
                .build()
        }

    override fun onMacroConstructionFinished(
        stage: CfirMacroConstructionStage,
        mode: MacroConstructionService.Mode?,
        outcome: CfirMacroExpansionOutcome?,
        fileCount: Int,
        surfaceCount: Int,
        elapsedNanos: Long,
    ) {
        durations[stage]?.record(elapsedNanos / NANOS_PER_MILLI)
        runs[stage]?.add(1)
        if (stage != CfirMacroConstructionStage.EXPANSION) return

        // 文件数与 surface 数属于整次 construction 的规模，索引/绑定两段重复累加会让总量翻倍。
        files.add(fileCount.toLong())
        surfaces.add(surfaceCount.toLong())
        outcome?.let { outcomes[it]?.add(1) }
    }
}
