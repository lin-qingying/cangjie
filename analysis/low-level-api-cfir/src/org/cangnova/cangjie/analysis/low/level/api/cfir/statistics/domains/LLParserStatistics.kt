package org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.domains

import io.opentelemetry.api.metrics.LongCounter
import io.opentelemetry.api.metrics.LongHistogram
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.DURATION_BUCKETS_MS
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLSpanTracker
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsScopes
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsService
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsSpanAttributes
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.NANOS_PER_MILLI
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.getMeter
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.getTracer
import org.cangnova.cangjie.parsing.CangjiePsiParseKind
import org.cangnova.cangjie.parsing.CangjiePsiParseTimingService

/**
 * PSI 解析耗时统计域。
 *
 * 回答 IDE 最常见的一类卡顿："打开/跳转到这个文件时解析花了多久"。按解析入口分桶，
 * 因为整文件解析与补全里的片段解析成本差一个量级。
 *
 * 注意与 [org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.domains.LLRawBuildStatistics]
 * 的 LightTree `parse` 阶段不重复：那条测的是 `LightTree2Cfir` 内的 LightTree 构建，
 * 本域测的是平台驱动的 PSI AST 构建（`CjFileElementType.doParseContents` 等）。两者是不同
 * 的解析路径——PSI 解析发生在 raw build 之前，由平台在需要 AST 时触发。
 *
 * 与 Kotlin 对照：Kotlin 的 `LLStatisticsService` 不统计 PSI 解析；
 * 这是仓颉为定位 IDE 卡顿补出的域。
 */
class LLParserStatistics(statisticsService: LLStatisticsService) :
    CangjiePsiParseTimingService(),
    LLStatisticsDomain {
    /**
     * 解析耗时的 meter 名字。
     */
    private val meter = statisticsService.openTelemetry.getMeter(LLStatisticsScopes.Parse)

    /**
     * 解析链路 span 记录器。
     */
    private val spans = LLSpanTracker(statisticsService.openTelemetry.getTracer(LLStatisticsScopes.Parse))

    /**
     * 按入口分桶的解析耗时直方图（毫秒）。
     */
    private val durations: Map<CangjiePsiParseKind, LongHistogram> =
        CangjiePsiParseKind.entries.associateWith { kind ->
            meter.histogramBuilder(LLStatisticsScopes.Parse.duration(kind))
                .setDescription("PSI parse duration for ${kind.metricSuffix} entry points")
                .setUnit("ms")
                .ofLongs()
                .setExplicitBucketBoundariesAdvice(DURATION_BUCKETS_MS)
                .build()
        }

    /**
     * 按入口分桶的解析次数。
     */
    private val runs: Map<CangjiePsiParseKind, LongCounter> =
        CangjiePsiParseKind.entries.associateWith { kind ->
            meter.counterBuilder(LLStatisticsScopes.Parse.runs(kind))
                .setDescription("Number of PSI parses for ${kind.metricSuffix} entry points")
                .build()
        }

    override fun onParseStarted(kind: CangjiePsiParseKind) {
        spans.start(kind, LLStatisticsScopes.Parse.kind(kind)) {
            setAttribute(LLStatisticsSpanAttributes.parseKind, kind.metricSuffix)
        }
    }

    override fun onParseFinished(kind: CangjiePsiParseKind, elapsedNanos: Long, succeeded: Boolean) {
        durations[kind]?.record(elapsedNanos / NANOS_PER_MILLI)
        runs[kind]?.add(1)
        spans.end(kind) {
            setAttribute(LLStatisticsSpanAttributes.parseSucceeded, succeeded)
        }
    }
}
