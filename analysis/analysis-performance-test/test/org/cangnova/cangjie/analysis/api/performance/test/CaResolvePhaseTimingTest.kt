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
 * 文件数与执行次数写进了 SDK，并且指标名落在 `cangjie.analysis.resolve.phases.*` 命名空间下。
 */
class CaResolvePhaseTimingTest : AbstractAnalysisApiPerformanceTest(
    "analysis/analysis-performance-test/testData/performance"
) {
    /**
     * 收集诊断会把声明逐阶段推进到目标阶段，随后每个阶段都应留下耗时采样。
     */
    @Test
    fun phaseDurationsAreRecordedForFullResolve(mainFile: CjFile) {
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

            val filesName = LLStatisticsMetricNames.resolvePhaseFiles(phase)
            val files = countersAfter.getValue(filesName) - countersBefore.getValue(filesName)
            assertTrue(files >= 1, "阶段 ${phase.name} 必须记录处理文件数，实际 delta=$files")
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