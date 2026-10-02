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
import org.cangnova.cangjie.cfir.builder.CfirRawBuildStage
import org.cangnova.cangjie.cfir.builder.CfirRawBuildTimingObserver

/**
 * raw CFIR 构建耗时统计域。
 *
 * 数据来源是两条文件级 raw 构建入口：`PsiRawCfirBuilder.buildCfirFile` 与
 * `LightTreeRawCfirDeclarationBuilder.buildCfirFile`（解析段来自 `LightTree2Cfir` 的
 * LightTree 解析）。IDE 首开文件走哪条路径由 LightTree 可用性决定，两条路径耗时特征不同；
 * 且"解析慢"与"转换慢"是两类不同的优化目标，因此按「来源 × 阶段」记录耗时（毫秒直方图）
 * 与执行次数。
 *
 * 次数再按 body 构建策略（立即 / 延迟）拆桶：两种模式的单次成本差异极大，合并计数会让
 * 平均值失去诊断意义。解析阶段不消费 body 策略，只登记一个 n/a 桶。
 *
 * PSI 路径不产生 `parse` 采样：PSI 解析由 IntelliJ 平台的文件加载完成，本项目无法在其外侧
 * 取到耗时，指标缺失是事实而非漏埋。
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
     * 耗时直方图的分段键：来源 × 阶段。
     */
    private data class Segment(val source: CfirRawBuildSource, val stage: CfirRawBuildStage)

    /**
     * 次数计数的分段键：在 [Segment] 基础上再加 body 策略。
     */
    private data class RunKey(
        val source: CfirRawBuildSource,
        val stage: CfirRawBuildStage,
        val bodyBuildingMode: BodyBuildingMode?,
    )

    /**
     * 耗时直方图覆盖的「来源 × 阶段」全集。
     */
    private val segments: List<Segment> = CfirRawBuildSource.entries.flatMap { source ->
        CfirRawBuildStage.entries.map { stage -> Segment(source, stage) }
    }

    /**
     * 次数计数覆盖的「来源 × 阶段 × body 策略」全集。
     */
    private val runKeys: List<RunKey> = CfirRawBuildSource.entries.flatMap { source ->
        CfirRawBuildStage.entries.flatMap { stage ->
            val modes: List<BodyBuildingMode?> = if (stage == CfirRawBuildStage.PARSE) {
                listOf(null)
            } else {
                BodyBuildingMode.entries
            }
            modes.map { mode -> RunKey(source, stage, mode) }
        }
    }

    /**
     * 按「来源 × 阶段」的耗时直方图（毫秒）。
     */
    private val durations: Map<Segment, LongHistogram> = segments.associateWith { segment ->
        meter.histogramBuilder(LLStatisticsScopes.RawBuild.duration(segment.source, segment.stage))
            .setDescription(
                "Raw CFIR build duration from ${segment.source.metricSuffix} at ${segment.stage.metricSuffix} stage"
            )
            .setUnit("ms")
            .ofLongs()
            .setExplicitBucketBoundariesAdvice(DURATION_BUCKETS_MS)
            .build()
    }

    /**
     * 按「来源 × 阶段 × body 策略」的执行次数。
     */
    private val runs: Map<RunKey, LongCounter> = runKeys.associateWith { key ->
        meter.counterBuilder(LLStatisticsScopes.RawBuild.runs(key.source, key.stage))
            .setDescription(
                "Number of ${key.stage.metricSuffix} executions from ${key.source.metricSuffix} " +
                    "(bodyBuildingMode=${key.bodyBuildingMode?.name ?: "n/a"})"
            )
            .build()
    }

    override fun onRawBuildFinished(
        source: CfirRawBuildSource,
        stage: CfirRawBuildStage,
        bodyBuildingMode: BodyBuildingMode?,
        elapsedNanos: Long,
    ) {
        durations[Segment(source, stage)]?.record(elapsedNanos / NANOS_PER_MILLI)
        runs[RunKey(source, stage, bodyBuildingMode)]?.add(1)
    }
}
