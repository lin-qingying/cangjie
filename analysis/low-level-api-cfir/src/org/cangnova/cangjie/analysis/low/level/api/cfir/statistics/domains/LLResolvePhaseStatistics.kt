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
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.resolve.transformers.CfirResolvePhaseTimingObserver
import org.cangnova.cangjie.cfir.resolve.transformers.CfirResolvePhaseWork

/**
 * 语义解析阶段耗时统计域。
 *
 * 数据来源是两条阶段驱动路径：`CfirTotalResolveProcessor` 的全量解析阶段循环，
 * 与 `LLCfirModuleLazyDeclarationResolver` 的按需解析阶段循环。每个阶段记录一次耗时
 * （毫秒直方图）与执行次数；工作量按路径分开计数：全量解析记覆盖的文件数，
 * 按需解析记推进的声明数。
 *
 * 与 Kotlin 对照：`LLStatisticsService` 只统计会话与符号提供者两个域，没有阶段耗时；
 * 阶段耗时是本项目为定位语义解析瓶颈补出的域，不改动已有域的名称与含义。
 */
class LLResolvePhaseStatistics internal constructor(
    statisticsService: LLStatisticsService,
) : LLStatisticsDomain, CfirResolvePhaseTimingObserver {
    /**
     * 阶段耗时的 meter 名字。
     */
    private val meter = statisticsService.openTelemetry.getMeter(LLStatisticsScopes.Resolve)

    /**
     * 阶段链路 span 记录器：与阶段指标同源，回答"这一次解析里时间花在哪个阶段"。
     */
    private val spans = LLSpanTracker(statisticsService.openTelemetry.getTracer(LLStatisticsScopes.Resolve))

    /**
     * 阶段耗时直方图（毫秒），按阶段名分桶。
     */
    private val phaseDurations: Map<CfirResolvePhase, LongHistogram> =
        CfirResolvePhase.entries.filterNot { it.noProcessor }.associateWith { phase ->
            meter.histogramBuilder(LLStatisticsScopes.Resolve.Phases.duration(phase))
                .setDescription("Semantic resolve phase duration for ${phase.name}")
                .setUnit("ms")
                .ofLongs()
                .setExplicitBucketBoundariesAdvice(DURATION_BUCKETS_MS)
                .build()
        }

    /**
     * 阶段执行次数。
     */
    private val phaseRuns: Map<CfirResolvePhase, LongCounter> =
        CfirResolvePhase.entries.filterNot { it.noProcessor }.associateWith { phase ->
            meter.counterBuilder(LLStatisticsScopes.Resolve.Phases.runs(phase))
                .setDescription("Number of executions of the ${phase.name} resolve phase")
                .build()
        }

    /**
     * 阶段覆盖的文件数，只由全量解析路径累加。
     */
    private val phaseFiles: Map<CfirResolvePhase, LongCounter> =
        CfirResolvePhase.entries.filterNot { it.noProcessor }.associateWith { phase ->
            meter.counterBuilder(LLStatisticsScopes.Resolve.Phases.files(phase))
                .setDescription("Number of CFIR files processed by the ${phase.name} resolve phase in full resolve")
                .build()
        }

    /**
     * 阶段推进的声明数，只由按需解析路径累加。
     */
    private val phaseDeclarations: Map<CfirResolvePhase, LongCounter> =
        CfirResolvePhase.entries.filterNot { it.noProcessor }.associateWith { phase ->
            meter.counterBuilder(LLStatisticsScopes.Resolve.Phases.declarations(phase))
                .setDescription("Number of declarations resolved by the ${phase.name} resolve phase in lazy resolve")
                .build()
        }

    override fun onPhaseStarted(phase: CfirResolvePhase) {
        // 耗时统一在 onPhaseFinished 记录；开始回调用于打开链路 span。
        spans.start(phase, LLStatisticsScopes.Resolve.Phases.span(phase)) {
            setAttribute(LLStatisticsSpanAttributes.resolvePhase, phase.name)
        }
    }

    override fun onPhaseFinished(phase: CfirResolvePhase, work: CfirResolvePhaseWork, elapsedNanos: Long) {
        phaseDurations[phase]?.record(elapsedNanos / NANOS_PER_MILLI)
        phaseRuns[phase]?.add(1)
        phaseFiles[phase]?.add(work.files.toLong())
        phaseDeclarations[phase]?.add(work.declarations.toLong())
        spans.end(phase) {
            setAttribute(LLStatisticsSpanAttributes.resolveFiles, work.files.toLong())
            setAttribute(LLStatisticsSpanAttributes.resolveDeclarations, work.declarations.toLong())
        }
    }
}
