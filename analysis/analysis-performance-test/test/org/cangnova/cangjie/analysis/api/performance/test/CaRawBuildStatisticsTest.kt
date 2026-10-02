package org.cangnova.cangjie.analysis.api.performance.test

import org.cangnova.cangjie.analysis.api.components.CaDiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsMetricNames
import org.cangnova.cangjie.cfir.builder.CfirRawBuildSource
import org.cangnova.cangjie.cfir.builder.CfirRawBuildStage
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * raw CFIR 构建耗时统计的接线测试。
 *
 * Analysis API 的 lazy resolve 经 `LLCfirFileBuilder` 用 PSI 路径构建 raw CFIR，
 * 因此收集诊断后必须能在 `rawBuild.psi.convert.*` 下读到耗时采样。LightTree 路径的
 * 解析与转换两段由 `light-tree2cfir` 模块的 seam 测试覆盖（分析宿主不会走那条路径）。
 */
class CaRawBuildStatisticsTest : AbstractAnalysisApiPerformanceTest(
    "analysis/analysis-performance-test/testData/performance"
) {
    /**
     * 收集诊断会触发 PSI raw CFIR 转换，随后该来源与阶段必须有耗时采样与执行计数。
     */
    @Test
    fun psiRawBuildConvertDurationIsRecorded(mainFile: CjFile) {
        val pointsBefore = CaPerformanceTestTelemetry.collectHistogramPointCounts()
        val countersBefore = CaPerformanceTestTelemetry.collectLongCounters()

        analyzeForTest(mainFile) {
            mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
        }

        val pointsAfter = CaPerformanceTestTelemetry.collectHistogramPointCounts()
        val countersAfter = CaPerformanceTestTelemetry.collectLongCounters()

        val source = CfirRawBuildSource.PSI
        val stage = CfirRawBuildStage.CONVERT
        val points = pointsAfter.getValue(LLStatisticsMetricNames.rawBuildDuration(source, stage)) -
            pointsBefore.getValue(LLStatisticsMetricNames.rawBuildDuration(source, stage))
        assertTrue(points >= 1, "PSI raw 转换必须记录至少一次耗时，实际 delta=$points")

        val runs = countersAfter.getValue(LLStatisticsMetricNames.rawBuildRuns(source, stage)) -
            countersBefore.getValue(LLStatisticsMetricNames.rawBuildRuns(source, stage))
        assertEquals(points, runs, "每个阶段执行都必须恰好记录一个耗时采样")
    }
}
