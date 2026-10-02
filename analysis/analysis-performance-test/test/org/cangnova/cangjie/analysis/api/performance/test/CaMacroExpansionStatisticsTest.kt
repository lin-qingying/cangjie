package org.cangnova.cangjie.analysis.api.performance.test

import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsMetricNames
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.domains.LLMacroExpansionStatistics
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirMacroExpansionOutcome
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionService
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * macro construction 统计域的接线测试。
 *
 * 分析宿主不运行真实宏 construction（宏由前端/LSP 路径展开），因此这里直接驱动统计域的
 * 回调，验证"耗时 + 次数 + 结果归类（含降级）"的指标映射；回调时机的正确性由
 * `cfir/analysis-tests` 的 seam 测试覆盖。
 */
class CaMacroExpansionStatisticsTest : AbstractAnalysisApiPerformanceTest(
    "analysis/analysis-performance-test/testData/performance"
) {
    /**
     * 五种结果归类各驱动一次后，耗时、次数、文件数、surface 数与每个结果计数都必须对应增加。
     */
    @Test
    fun macroExpansionOutcomesAreCounted(mainFile: CjFile) {
        val domain = LLMacroExpansionStatistics(lowLevelStatisticsService)
        // 宏指标在测试体之前没有任何采样点（分析宿主不跑宏 construction），
        // before 快照里必然没有这些 key，因此取增量必须容忍缺键。
        val pointsBefore = CaPerformanceTestTelemetry.collectHistogramPointCounts()
        val countersBefore = CaPerformanceTestTelemetry.collectLongCounters()

        CfirMacroExpansionOutcome.entries.forEach { outcome ->
            domain.onMacroExpansionFinished(
                mode = MacroConstructionService.Mode.DEGRADED,
                outcome = outcome,
                fileCount = 2,
                surfaceCount = 3,
                elapsedNanos = 3_000_000L,
            )
        }

        val pointsAfter = CaPerformanceTestTelemetry.collectHistogramPointCounts()
        val countersAfter = CaPerformanceTestTelemetry.collectLongCounters()

        fun counterDelta(name: String): Long = (countersAfter[name] ?: 0L) - (countersBefore[name] ?: 0L)
        val durationName = LLStatisticsMetricNames.macroExpandDuration
        val pointDelta = (pointsAfter[durationName] ?: 0L) - (pointsBefore[durationName] ?: 0L)
        val outcomeCount = CfirMacroExpansionOutcome.entries.size

        assertEquals(outcomeCount.toLong(), pointDelta, "每次 construction 必须记录一个耗时采样")
        assertEquals(outcomeCount.toLong(), counterDelta(LLStatisticsMetricNames.macroExpandRuns), "construction 次数必须对应增加")
        assertEquals(outcomeCount * 2L, counterDelta(LLStatisticsMetricNames.macroExpandFiles), "文件数必须累加")
        assertEquals(outcomeCount * 3L, counterDelta(LLStatisticsMetricNames.macroExpandSurfaces), "surface 数必须累加")
        CfirMacroExpansionOutcome.entries.forEach { outcome ->
            assertEquals(1L, counterDelta(LLStatisticsMetricNames.macroExpandOutcome(outcome)), "结果 ${outcome.metricSuffix} 必须计数一次")
        }
        assertTrue(
            counterDelta(LLStatisticsMetricNames.macroExpandOutcome(CfirMacroExpansionOutcome.DEGRADED)) >= 1,
            "降级次数必须单独可见",
        )
    }
}
