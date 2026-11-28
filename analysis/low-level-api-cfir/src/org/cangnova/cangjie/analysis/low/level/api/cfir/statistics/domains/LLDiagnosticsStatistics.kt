package org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.domains

import io.opentelemetry.api.metrics.LongCounter
import io.opentelemetry.api.metrics.LongHistogram
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.CaDiagnosticCheckerSet
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.DURATION_BUCKETS_MS
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLSpanTracker
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsScopes
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsService
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsSpanAttributes
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.NANOS_PER_MILLI
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.getMeter
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.getTracer

/**
 * IDE 请求级诊断收集耗时统计域。
 *
 * 这是 IDE 卡顿投诉里"我在这个文件里按了检查，为什么慢"的直接答案。四个维度各自回答一个问题：
 *
 * - `collection`：文件级 `collectDiagnostics` 的端到端耗时，用户实际等待的那一次；
 * - `elementCollection`：元素级 `getDiagnostics`，高频且单次很轻；
 * - `structureBuild`：文件结构首次构建，是 `collection` 的主要成本之一，单独可见才能把它
 *   与 checker 成本分开；
 * - `checkerPass`：单个 structure element 上某个 checker 集合的实际遍历耗时。
 *
 * `collection` 会包含 `structureBuild` 与 `checkerPass`，三层嵌套是有意的：总耗时定位问题，
 * 分层耗时定位责任方。
 *
 * 与 Kotlin 对照：Kotlin 的 `LLStatisticsService` 不统计诊断收集；
 * 这是仓颉为定位 IDE 卡顿补出的域。
 */
class LLDiagnosticsStatistics(statisticsService: LLStatisticsService) : LLStatisticsDomain {
    /**
     * 诊断收集耗时的 meter 名字。
     */
    private val meter = statisticsService.openTelemetry.getMeter(LLStatisticsScopes.Diagnostics)

    /**
     * 诊断收集链路 span 记录器。
     */
    private val spans = LLSpanTracker(statisticsService.openTelemetry.getTracer(LLStatisticsScopes.Diagnostics))

    /**
     * 一次诊断收集的三个维度：耗时、次数、产出诊断数。
     */
    private class Counter(val duration: LongHistogram, val runs: LongCounter, val diagnostics: LongCounter)

    /**
     * 文件级诊断收集的直方图与计数器。
     */
    private val collection = counter(
        LLStatisticsScopes.Diagnostics.Collection.duration(),
        LLStatisticsScopes.Diagnostics.Collection.runs(),
        LLStatisticsScopes.Diagnostics.Collection.diagnostics(),
        "File-level diagnostics collection",
    )

    /**
     * 元素级诊断收集的直方图与计数器。
     */
    private val elementCollection = counter(
        LLStatisticsScopes.Diagnostics.ElementCollection.duration(),
        LLStatisticsScopes.Diagnostics.ElementCollection.runs(),
        LLStatisticsScopes.Diagnostics.ElementCollection.diagnostics(),
        "Element-level diagnostics collection",
    )

    /**
     * 文件结构首次构建的直方图与计数器。
     */
    private val structureBuild = counter(
        LLStatisticsScopes.Diagnostics.StructureBuild.duration(),
        LLStatisticsScopes.Diagnostics.StructureBuild.runs(),
        LLStatisticsScopes.Diagnostics.StructureBuild.diagnostics(),
        "File structure build",
    )

    /**
     * 按 checker 集合分桶的"单个 structure element 完整收集"直方图与计数器。
     *
     * 只登记 [CaDiagnosticCheckerSet] 的三个单集合桶：组合 filter（`DiagnosticCheckerFilter.plus`）
     * 在该维度上没有唯一取值，调用方对组合 filter 不上报，而不是硬塞进某个集合。
     * 单次遍历的分阶段耗时由 `LLDiagnosticPassStatistics` 负责，两者正交。
     */
    private val structureElements: Map<CaDiagnosticCheckerSet, Counter> =
        CaDiagnosticCheckerSet.entries.associateWith { set ->
            val scope = LLStatisticsScopes.Diagnostics.StructureElement
            counter(
                scope.duration(set),
                scope.runs(set),
                scope.diagnostics(set),
                "Structure element diagnostics collection (${set.metricSuffix})",
            )
        }

    /**
     * 为一组指标名创建耗时直方图与两个计数器。
     *
     * 参数是三个完整指标名而不是 scope 对象：`checkerPass` 的三个指标都带 checker 集合参数，
     * 没有"该集合的 scope 对象"可传，直接传名字让命名规则留在 [LLStatisticsScopes] 一处。
     */
    private fun counter(
        durationName: String,
        runsName: String,
        diagnosticsName: String,
        description: String,
    ): Counter {
        val duration = meter.histogramBuilder(durationName)
            .setDescription("$description duration")
            .setUnit("ms")
            .ofLongs()
            .setExplicitBucketBoundariesAdvice(DURATION_BUCKETS_MS)
            .build()
        val runs = meter.counterBuilder(runsName).setDescription("Number of $description operations").build()
        val diagnostics =
            meter.counterBuilder(diagnosticsName).setDescription("Number of diagnostics produced by $description").build()
        return Counter(duration, runs, diagnostics)
    }

    /**
     * 开始一次文件级诊断收集。
     */
    fun onFileCollectionStarted() =
        spans.start(LLStatisticsScopes.Diagnostics.Collection, LLStatisticsScopes.Diagnostics.Collection.name)

    /**
     * 开始一次元素级诊断收集。
     */
    fun onElementCollectionStarted() =
        spans.start(LLStatisticsScopes.Diagnostics.ElementCollection, LLStatisticsScopes.Diagnostics.ElementCollection.name)

    /**
     * 开始一次文件结构首次构建。
     */
    fun onStructureBuildStarted() =
        spans.start(LLStatisticsScopes.Diagnostics.StructureBuild, LLStatisticsScopes.Diagnostics.StructureBuild.name)

    /**
     * 开始一次单个 structure element 上、某 checker 集合的完整收集。
     */
    fun onStructureElementCollectionStarted(set: CaDiagnosticCheckerSet) =
        spans.start(set, LLStatisticsScopes.Diagnostics.StructureElement.set(set))

    /**
     * 记录文件级诊断收集的耗时与产出，并结束对应的链路 span。
     */
    fun onFileCollectionFinished(elapsedNanos: Long, diagnosticCount: Int) {
        record(collection, elapsedNanos, diagnosticCount)
        spans.end(LLStatisticsScopes.Diagnostics.Collection) {
            setAttribute(LLStatisticsSpanAttributes.diagnosticsCount, diagnosticCount.toLong())
        }
    }

    /**
     * 记录元素级诊断收集的耗时与产出，并结束对应的链路 span。
     */
    fun onElementCollectionFinished(elapsedNanos: Long, diagnosticCount: Int) {
        record(elementCollection, elapsedNanos, diagnosticCount)
        spans.end(LLStatisticsScopes.Diagnostics.ElementCollection) {
            setAttribute(LLStatisticsSpanAttributes.diagnosticsCount, diagnosticCount.toLong())
        }
    }

    /**
     * 记录文件结构首次构建的耗时，并结束对应的链路 span。结构构建不产出诊断，因此诊断数为 0。
     */
    fun onStructureBuildFinished(elapsedNanos: Long) {
        record(structureBuild, elapsedNanos, 0)
        spans.end(LLStatisticsScopes.Diagnostics.StructureBuild)
    }

    /**
     * 记录单个 structure element 上某个 checker 集合的完整收集耗时与产出。
     */
    fun onStructureElementCollectionFinished(set: CaDiagnosticCheckerSet, elapsedNanos: Long, diagnosticCount: Int) {
        val counter = structureElements[set]
        if (counter != null) {
            record(counter, elapsedNanos, diagnosticCount)
        }
        spans.end(set) {
            setAttribute(LLStatisticsSpanAttributes.diagnosticsCount, diagnosticCount.toLong())
        }
    }

    /**
     * 把一次操作写入耗时、次数与诊断数三个指标。
     */
    private fun record(counter: Counter, elapsedNanos: Long, diagnosticCount: Int) {
        counter.duration.record(elapsedNanos / NANOS_PER_MILLI)
        counter.runs.add(1)
        counter.diagnostics.add(diagnosticCount.toLong())
    }
}
