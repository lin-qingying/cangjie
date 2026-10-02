package org.cangnova.cangjie.analysis.api.performance.test

import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsMetricNames
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.domains.LLMacroConstructionStatistics
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirMacroConstructionStage
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirMacroExpansionOutcome
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionService
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * macro construction 统计域的接线测试。
 *
 * 分析宿主不运行真实宏 construction（宏由前端/LSP 路径展开），因此这里直接驱动统计域的
 * 回调，验证"三段耗时 + 次数 + 展开段的结果归类"映射；阶段上报时机与真实调用方由
 * `cfir/analysis-tests` 的 seam 测试覆盖。
 */
class CaMacroConstructionStatisticsTest : AbstractAnalysisApiPerformanceTest(
    "analysis/analysis-performance-test/testData/performance"
) {
    /**
     * 三个阶段各驱动一次后，耗时、次数、文件数、surface 数与每个结果计数都必须对应增加。
     */
    @Test
    fun macroConstructionStagesAreCounted(mainFile: CjFile) {
        val domain = LLMacroConstructionStatistics(lowLevelStatisticsService)
        val pointsBefore = CaPerformanceTestTelemetry.collectHistogramPointCounts()
        val countersBefore = CaPerformanceTestTelemetry.collectLongCounters()

        CfirMacroConstructionStage.entries.forEach { stage ->
            val isExpansion = stage == CfirMacroConstructionStage.EXPANSION
            domain.onMacroConstructionFinished(
                stage = stage,
                mode = if (isExpansion) MacroConstructionService.Mode.DEGRADED else null,
                outcome = if (isExpansion) CfirMacroExpansionOutcome.DEGRADED else null,
                fileCount = 2,
                surfaceCount = 3,
                elapsedNanos = 3_000_000L,
            )
        }

        val pointsAfter = CaPerformanceTestTelemetry.collectHistogramPointCounts()
        val countersAfter = CaPerformanceTestTelemetry.collectLongCounters()

        fun counterDelta(name: String): Long = (countersAfter[name] ?: 0L) - (countersBefore[name] ?: 0L)
        val stageCount = CfirMacroConstructionStage.entries.size

        CfirMacroConstructionStage.entries.forEach { stage ->
            val durationName = LLStatisticsMetricNames.macroConstructionDuration(stage)
            val points = (pointsAfter[durationName] ?: 0L) - (pointsBefore[durationName] ?: 0L)
            assertEquals(1L, points, "阶段 ${stage.metricSuffix} 必须记录一个耗时采样")
            assertEquals(1L, counterDelta(LLStatisticsMetricNames.macroConstructionRuns(stage)), "阶段 ${stage.metricSuffix} 次数必须加一")
        }
        // 文件数与 surface 数属于整次 construction 的规模，只在展开段累加一次。
        assertEquals(2L, counterDelta(LLStatisticsMetricNames.macroExpandFiles), "文件数只能在展开段累加一次")
        assertEquals(3L, counterDelta(LLStatisticsMetricNames.macroExpandSurfaces), "surface 数只能在展开段累加一次")
        assertEquals(
            1L,
            counterDelta(LLStatisticsMetricNames.macroExpandOutcome(CfirMacroExpansionOutcome.DEGRADED)),
            "降级次数必须单独可见",
        )
        assertEquals(stageCount.toLong(), CfirMacroConstructionStage.entries.size.toLong(), "阶段数必须与枚举一致")
    }
}
