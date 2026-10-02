package org.cangnova.cangjie.analysis.api.performance.test

import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.CaModuleKind
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsMetricNames
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.domains.LLDeserializationStatistics
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.domains.LLSessionStatistics
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * session 创建、scope session 与 stub 反序列化统计域的接线测试。
 *
 * 分析宿主不会在本用例内真的批量创建 session 或反序列化库类（前者受工程结构约束，
 * 后者需要库模块），因此这里直接驱动统计域的回调，核对指标映射与"未识别模块种类不计入
 * 任何桶"这条边界规则；调用时机的正确性由 `low-level-api-cfir` 与 `analysis-api-cfir`
 * 的既有接线保证。
 */
class CaSessionAndDeserializationStatisticsTest : AbstractAnalysisApiPerformanceTest(
    "analysis/analysis-performance-test/testData/performance"
) {
    /**
     * 已知模块种类各自记一次时长与次数；`UNKNOWN` 必须被忽略；scope session 与反序列化各记一次。
     */
    @Test
    fun sessionCreationAndDeserializationAreCounted(mainFile: CjFile) {
        val sessions = LLSessionStatistics(lowLevelStatisticsService)
        val deserialization = LLDeserializationStatistics(lowLevelStatisticsService)
        val countersBefore = CaPerformanceTestTelemetry.collectLongCounters()
        val pointsBefore = CaPerformanceTestTelemetry.collectHistogramPointCounts()

        val knownKinds = CaModuleKind.entries.filter { it != CaModuleKind.UNKNOWN }
        knownKinds.forEach { kind ->
            sessions.onSessionCreated(kind, 2_000_000L)
            sessions.onScopeSessionCreated()
        }
        sessions.onSessionCreated(CaModuleKind.UNKNOWN, 1_000_000L)
        deserialization.onClassLikeDeserialized(4_000_000L)

        val countersAfter = CaPerformanceTestTelemetry.collectLongCounters()
        val pointsAfter = CaPerformanceTestTelemetry.collectHistogramPointCounts()

        fun runs(name: String): Long = (countersAfter[name] ?: 0L) - (countersBefore[name] ?: 0L)
        fun points(name: String): Long = (pointsAfter[name] ?: 0L) - (pointsBefore[name] ?: 0L)

        knownKinds.forEach { kind ->
            assertEquals(1L, points(LLStatisticsMetricNames.sessionCreationDuration(kind)), "${kind.metricSuffix} session 创建耗时")
            assertEquals(1L, runs(LLStatisticsMetricNames.sessionCreationRuns(kind)), "${kind.metricSuffix} session 创建次数")
        }
        assertEquals(
            knownKinds.size.toLong(),
            runs(LLStatisticsMetricNames.scopeSessionsCreated),
            "每次 session 创建驱动一次 scope session 创建",
        )
        assertEquals(1L, points(LLStatisticsMetricNames.deserializationClassLikeDuration), "stub 反序列化耗时")
        assertEquals(1L, runs(LLStatisticsMetricNames.deserializationClassLikeRuns), "stub 反序列化次数")
    }
}
