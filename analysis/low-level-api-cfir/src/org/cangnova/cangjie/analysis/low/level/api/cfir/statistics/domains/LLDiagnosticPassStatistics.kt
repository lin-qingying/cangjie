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
import org.cangnova.cangjie.cfir.analysis.collectors.CfirDiagnosticPassTimingObserver
import org.cangnova.cangjie.cfir.analysis.collectors.DiagnosticCollectionPhase

/**
 * 诊断遍历分阶段耗时统计域。
 *
 * 与 `LLDiagnosticsStatistics` 的分工：后者按 checker 集合（默认 / 额外 / 实验性）计量
 * "一个 structure element 上的完整收集"，本域按遍历阶段（SEMA / POST_SEMA）计量"每次遍历
 * 本身"。两者正交：前者回答"哪个 checker 集合慢"，本域回答"慢在常规检查还是后续检查"。
 *
 * 与 Kotlin 对照：Kotlin 的 `LLStatisticsService` 不统计诊断遍历；
 * 这是仓颉为定位 IDE 卡顿补出的域。
 */
class LLDiagnosticPassStatistics(statisticsService: LLStatisticsService) :
    LLStatisticsDomain,
    CfirDiagnosticPassTimingObserver {

    /**
     * 遍历耗时的 meter 名字。
     */
    private val meter = statisticsService.openTelemetry.getMeter(LLStatisticsScopes.Diagnostics)

    /**
     * 诊断遍历链路 span 记录器。
     */
    private val spans = LLSpanTracker(statisticsService.openTelemetry.getTracer(LLStatisticsScopes.Diagnostics))

    /**
     * 各阶段耗时直方图（毫秒）。
     */
    private val durations: Map<DiagnosticCollectionPhase, LongHistogram> =
        DiagnosticCollectionPhase.entries.associateWith { phase ->
            val scope = LLStatisticsScopes.Diagnostics.Pass
            meter.histogramBuilder(scope.duration(phase))
                .setDescription("Diagnostic traversal duration at ${phase.name.lowercase()} stage")
                .setUnit("ms")
                .ofLongs()
                .setExplicitBucketBoundariesAdvice(DURATION_BUCKETS_MS)
                .build()
        }

    /**
     * 各阶段执行次数。
     */
    private val runs: Map<DiagnosticCollectionPhase, LongCounter> =
        DiagnosticCollectionPhase.entries.associateWith { phase ->
            meter.counterBuilder(LLStatisticsScopes.Diagnostics.Pass.runs(phase))
                .setDescription("Number of diagnostic traversals at ${phase.name.lowercase()} stage")
                .build()
        }

    override fun onDiagnosticPassStarted(phase: DiagnosticCollectionPhase) {
        spans.start(phase, LLStatisticsScopes.Diagnostics.Pass.phase(phase)) {
            setAttribute(LLStatisticsSpanAttributes.diagnosticsPhase, phase.name.lowercase())
        }
    }

    override fun onDiagnosticPassFinished(phase: DiagnosticCollectionPhase, elapsedNanos: Long) {
        durations[phase]?.record(elapsedNanos / NANOS_PER_MILLI)
        runs[phase]?.add(1)
        spans.end(phase)
    }
}
