package org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.domains

import io.opentelemetry.api.metrics.LongCounter
import io.opentelemetry.api.metrics.LongHistogram
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.DURATION_BUCKETS_US
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsScopes
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsService
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.NANOS_PER_MICRO
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.getMeter
import org.cangnova.cangjie.cfir.analysis.collectors.CfirCheckerComponentKind
import org.cangnova.cangjie.cfir.analysis.collectors.CfirCheckerComponentTimingObserver

/**
 * 逐诊断组件耗时统计域。
 *
 * 回答的是"慢在哪一类检查"：声明检查器要跑几十个子检查器、错误节点组件基本是空转，两者在
 * [LLDiagnosticsStatistics] 的集合级耗时里完全混在一起。与既有三个诊断域的分工：
 *
 * - [LLDiagnosticsStatistics]：一个 structure element 上的完整收集（按 checker 集合分桶）；
 * - [LLDiagnosticPassStatistics]：一次遍历整体（按 SEMA / POST_SEMA 分桶）；
 * - 本域：这次遍历里各组件分别多久（按组件种类分桶）。
 *
 * **单位是微秒而不是毫秒。** 计量边界是"一个元素在一个组件上的检查"，基本都在亚毫秒量级；
 * 整毫秒会让绝大多数样本塌成 0、直方图 `_sum` 恒为 0。
 *
 * 与 Kotlin 对照：K2 没有这一层统计，仓颉为了定位"IDE 诊断卡在哪"补出本域。
 */
class LLCheckerComponentStatistics(statisticsService: LLStatisticsService) :
    LLStatisticsDomain,
    CfirCheckerComponentTimingObserver {

    /**
     * 诊断组件耗时的 meter。
     */
    private val meter = statisticsService.openTelemetry.getMeter(LLStatisticsScopes.Diagnostics)

    /**
     * 各组件的单次耗时直方图（微秒）。
     */
    private val durations: Map<CfirCheckerComponentKind, LongHistogram> =
        CfirCheckerComponentKind.entries.associateWith { kind ->
            meter.histogramBuilder(LLStatisticsScopes.Diagnostics.CheckerComponent.duration(kind))
                .setDescription("Duration of running the ${kind.metricSuffix} diagnostic component on one element")
                .setUnit("us")
                .ofLongs()
                .setExplicitBucketBoundariesAdvice(DURATION_BUCKETS_US)
                .build()
        }

    /**
     * 各组件被执行的元素数；与耗时相除即单次平均耗时。
     */
    private val runs: Map<CfirCheckerComponentKind, LongCounter> =
        CfirCheckerComponentKind.entries.associateWith { kind ->
            meter.counterBuilder(LLStatisticsScopes.Diagnostics.CheckerComponent.runs(kind))
                .setDescription("Number of elements dispatched to the ${kind.metricSuffix} diagnostic component")
                .build()
        }

    override fun onCheckerComponentFinished(kind: CfirCheckerComponentKind, elapsedNanos: Long) {
        durations[kind]?.record(elapsedNanos / NANOS_PER_MICRO)
        runs[kind]?.add(1)
    }
}