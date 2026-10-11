package org.cangnova.cangjie.analysis.api.performance.test

import org.cangnova.cangjie.analysis.api.components.CaDiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsMetricNames
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 语义解析阶段耗时统计的接线测试。
 *
 * 验证按需解析路径（IDE / Analysis API 收集诊断时走的阶段循环）确实把每个阶段的耗时、
 * 推进的声明数与执行次数写进了 SDK，并且指标名落在 `cangjie.analysis.resolve.phases.*` 命名空间下。
 */
class CaResolvePhaseTimingTest : AbstractAnalysisApiPerformanceTest(
    "analysis/analysis-performance-test/testData/performance"
) {
    /**
     * 收集诊断会把声明逐阶段推进到目标阶段，随后每个阶段都应留下耗时采样。
     *
     * 按需解析路径的工作量记在 `declarations` 上（`files` 只由全量解析路径累加），
     * 因此这里断言声明数而非文件数。
     */
    @Test
    fun phaseDurationsAreRecordedForLazyResolve(mainFile: CjFile) {
        val pointsBefore = CaPerformanceTestTelemetry.collectHistogramPointCounts()
        val countersBefore = CaPerformanceTestTelemetry.collectLongCounters()

        analyzeForTest(mainFile) {
            mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
        }

        val pointsAfter = CaPerformanceTestTelemetry.collectHistogramPointCounts()
        val countersAfter = CaPerformanceTestTelemetry.collectLongCounters()

        CfirResolvePhase.entries.filterNot { it.noProcessor }.forEach { phase ->
            val durationName = LLStatisticsMetricNames.resolvePhaseDuration(phase)
            val recorded = pointsAfter.getValue(durationName) - pointsBefore.getValue(durationName)
            assertTrue(recorded >= 1, "阶段 ${phase.name} 必须记录至少一次耗时，实际 delta=$recorded")

            val runsName = LLStatisticsMetricNames.resolvePhaseRuns(phase)
            val runs = countersAfter.getValue(runsName) - countersBefore.getValue(runsName)
            assertTrue(runs >= 1, "阶段 ${phase.name} 必须记录至少一次执行，实际 delta=$runs")

            val declarationsName = LLStatisticsMetricNames.resolvePhaseDeclarations(phase)
            val declarations = countersAfter.getValue(declarationsName) - countersBefore.getValue(declarationsName)
            assertTrue(declarations >= 1, "阶段 ${phase.name} 必须记录推进的声明数，实际 delta=$declarations")
        }
    }

    /**
     * 所有阶段指标都必须落在仓颉自己的命名空间下。
     */
    @Test
    fun phaseMetricNamesUseCangjieNamespace(mainFile: CjFile) {
        analyzeForTest(mainFile) {
            mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
        }

        val names = CaPerformanceTestTelemetry.collectHistogramPointCounts().keys +
            CaPerformanceTestTelemetry.collectLongCounters().keys
        val phaseNames = names.filter { it.contains(".resolve.phases.") }
        assertTrue(phaseNames.isNotEmpty(), "解析后必须能读到阶段指标，实际指标=$names")
        val foreign = phaseNames.filterNot { LLStatisticsMetricNames.isCangjieAnalysisMetric(it) }
        assertTrue(foreign.isEmpty(), "阶段指标必须以 ${LLStatisticsMetricNames.root} 为前缀，越界指标：$foreign")
    }
}