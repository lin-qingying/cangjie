package org.cangnova.cangjie.analysis.api.performance.test

import org.cangnova.cangjie.analysis.api.components.CaDiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.CaDiagnosticCheckerSet
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsMetricNames
import org.cangnova.cangjie.cfir.analysis.collectors.DiagnosticCollectionPhase
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * IDE 请求级诊断收集耗时统计的接线测试。
 *
 * 这是"IDE 卡顿投诉"最直接的对应物：用户在文件上按检查，等待的就是
 * `diagnostics.collection.duration`。本用例同时验证四个分解维度各自留有采样：文件结构首次
 * 构建、按 checker 集合的元素级收集，以及 SEMA / POST_SEMA 两段遍历——少任何一段，总耗时
 * 异常时都无法定位责任方。
 */
class CaDiagnosticsStatisticsTest : AbstractAnalysisApiPerformanceTest(
    "analysis/analysis-performance-test/testData/performance"
) {
    /**
     * 收集诊断后，端到端耗时、结构构建与被请求的 checker 集合都必须留下采样。
     */
    @Test
    fun diagnosticsCollectionStagesAreRecorded(mainFile: CjFile) {
        val pointsBefore = CaPerformanceTestTelemetry.collectHistogramPointCounts()
        val countersBefore = CaPerformanceTestTelemetry.collectLongCounters()

        analyzeForTest(mainFile) {
            mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
        }

        val pointsAfter = CaPerformanceTestTelemetry.collectHistogramPointCounts()
        val countersAfter = CaPerformanceTestTelemetry.collectLongCounters()

        fun points(name: String): Long = (pointsAfter[name] ?: 0L) - (pointsBefore[name] ?: 0L)
        fun runs(name: String): Long = (countersAfter[name] ?: 0L) - (countersBefore[name] ?: 0L)

        assertTrue(
            points(LLStatisticsMetricNames.diagnosticsCollectionDuration) >= 1,
            "文件级诊断收集必须记录耗时",
        )
        assertTrue(runs(LLStatisticsMetricNames.diagnosticsCollectionRuns) >= 1, "文件级诊断收集必须记次数")
        assertTrue(
            points(LLStatisticsMetricNames.diagnosticsStructureBuildDuration) >= 1,
            "文件结构首次构建必须记录耗时",
        )
        // EXTENDED_AND_COMMON 会请求 default 与 extra 两个 checker 集合。
        listOf(CaDiagnosticCheckerSet.DEFAULT, CaDiagnosticCheckerSet.EXTRA).forEach { set ->
            assertTrue(
                points(LLStatisticsMetricNames.diagnosticsStructureElementDuration(set)) >= 1,
                "checker 集合 ${set.metricSuffix} 必须记录元素级收集耗时",
            )
            assertTrue(
                runs(LLStatisticsMetricNames.diagnosticsStructureElementRuns(set)) >= 1,
                "checker 集合 ${set.metricSuffix} 必须记元素级收集次数",
            )
        }

        // 遍历分阶段：SEMA 常规检查必然发生；POST_SEMA 在文件无错误且集合非空时发生。
        // 本用例的测试数据无错误，且 default filter 会装配 post-sema 组件，因此两段都应出现。
        DiagnosticCollectionPhase.entries.forEach { phase ->
            assertTrue(
                points(LLStatisticsMetricNames.diagnosticsPassDuration(phase)) >= 1,
                "遍历阶段 ${phase.name.lowercase()} 必须记录耗时",
            )
            assertTrue(
                runs(LLStatisticsMetricNames.diagnosticsPassRuns(phase)) >= 1,
                "遍历阶段 ${phase.name.lowercase()} 必须记遍历次数",
            )
        }
    }
}
