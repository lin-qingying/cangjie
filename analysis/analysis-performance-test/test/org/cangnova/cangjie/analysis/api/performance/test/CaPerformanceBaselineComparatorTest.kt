package org.cangnova.cangjie.analysis.api.performance.test

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 基线选取与噪声判定的单测。
 *
 * 这里不碰任何 SDK：对比逻辑是纯函数，因此可以构造出"明显回归"、"抖动范围内的变化"、
 * "样本不足"这些在真实运行里要跑几十次才碰得上的情形，并断言判定是否符合预期。
 */
class CaPerformanceBaselineComparatorTest {
    /**
     * 造一条只有用例墙钟的运行记录。
     */
    private fun run(id: String, timestamp: Long, wallNanos: Long): CaPerformanceRunRecord =
        CaPerformanceRunRecord(
            runId = id,
            timestampMillis = timestamp,
            gitCommit = null,
            cases = listOf(
                CaPerformanceCaseRecord(
                    testClass = "SampleTest",
                    testMethod = "someCase",
                    wallNanos = wallNanos,
                    delta = CaPerformanceDelta.EMPTY,
                ),
            ),
        )

    /**
     * 造一条同时带计数与耗时采样的运行记录。
     */
    private fun runWithMetrics(id: String, timestamp: Long, calls: Long, totalNanos: Double, samples: Long) =
        CaPerformanceRunRecord(
            runId = id,
            timestampMillis = timestamp,
            gitCommit = null,
            cases = listOf(
                CaPerformanceCaseRecord(
                    testClass = "SampleTest",
                    testMethod = "someCase",
                    wallNanos = 1_000_000L,
                    delta = CaPerformanceDelta(
                        counters = mapOf("cangjie.analysis.analysisSessions.analyze.invocations" to calls),
                        durationNanosSum = mapOf("cangjie.analysis.resolve.phases.types.duration" to totalNanos),
                        histogramCounts = mapOf("cangjie.analysis.resolve.phases.types.duration" to samples),
                    ),
                ),
            ),
        )

    @Test
    fun medianHandlesOddAndEvenSampleCounts() {
        assertEquals(2.0, CaPerformanceBaselineComparator.median(listOf(1.0, 2.0, 3.0)), 1e-9)
        assertEquals(2.5, CaPerformanceBaselineComparator.median(listOf(1.0, 2.0, 3.0, 4.0)), 1e-9)
        assertEquals(0.0, CaPerformanceBaselineComparator.median(emptyList()), 1e-9)
    }

    @Test
    fun jitterNeedsEnoughSamplesAndZeroMiddleYieldsZero() {
        // 样本不足时不估计抖动，由固定下限兜底。
        assertEquals(0.0, CaPerformanceBaselineComparator.jitter(listOf(10.0, 20.0)), 1e-9)
        assertEquals(0.0, CaPerformanceBaselineComparator.jitter(listOf(0.0, 0.0, 0.0)), 1e-9)
        // 样本 10/20/15：极差 10，中位数 15。
        assertEquals(10.0 / 15.0, CaPerformanceBaselineComparator.jitter(listOf(10.0, 20.0, 15.0)), 1e-9)
    }

    @Test
    fun firstRunHasNoBaseline() {
        val current = run("r1", 1_000L, 1_000_000L)
        val report = CaPerformanceBaselineComparator.compare(current, emptyList())

        assertEquals(null, report.baselineRunId)
        assertEquals(1, report.cases.size)
        assertTrue(report.cases.single().signals.all { it.verdict == CaPerformanceVerdict.BASELINE_MISSING })
    }

    @Test
    fun secondRunCannotJudgeBecauseThereIsOnlyOneSample() {
        val current = run("r2", 2_000L, 5_000_000L)
        val report = CaPerformanceBaselineComparator.compare(current, listOf(run("r1", 1_000L, 1_000_000L)))

        val signal = report.cases.single().signals.single()
        assertEquals(CaPerformanceVerdict.INSUFFICIENT_DATA, signal.verdict)
        // 即使差了 5 倍也不能判回归——一次对比说明不了问题。
        assertEquals("上一次", signal.baselineDescription)
    }

    @Test
    fun stableHistoryTurnsABigChangeIntoAReggression() {
        val history = (1..4).map { run("r$it", it * 1_000L, 1_000_000L) }
        val current = run("now", 9_000L, 2_000_000L)

        val signal = CaPerformanceBaselineComparator.compare(current, history).cases.single().signals.single()

        assertEquals(CaPerformanceVerdict.REGRESSED, signal.verdict)
        assertEquals("近 4 次中位数", signal.baselineDescription)
        assertEquals(1.0, signal.relativeChange!!, 1e-9)
    }

    @Test
    fun jitterAbsorbsSmallChangesOnceHistoryExists() {
        // 历史在 100 与 110 之间抖动（约 10% 极差），本次 104 落在带内。
        val history = listOf(100.0, 110.0, 105.0).mapIndexed { index, value ->
            run("r$index", (index + 1) * 1_000L, (value * 1_000_000).toLong())
        }
        val current = run("now", 9_000L, 104_000_000L)

        val signal = CaPerformanceBaselineComparator.compare(current, history).cases.single().signals.single()

        assertEquals(CaPerformanceVerdict.NOISE, signal.verdict)
        assertTrue(signal.threshold >= 0.05, "阈值不应低于固定下限")
    }

    @Test
    fun caseMissingFromHistoryIsReportedAsMissing() {
        val history = listOf(
            CaPerformanceRunRecord(
                runId = "r1",
                timestampMillis = 1_000L,
                gitCommit = null,
                cases = listOf(
                    CaPerformanceCaseRecord("OtherTest", "other", 1L, CaPerformanceDelta.EMPTY),
                ),
            ),
        )
        val current = run("now", 2_000L, 1_000_000L)

        val signal = CaPerformanceBaselineComparator.compare(current, history).cases.single().signals.single()

        assertEquals(CaPerformanceVerdict.BASELINE_MISSING, signal.verdict)
    }

    @Test
    fun durationSignalsUseSampleMeanNotRawSum() {
        // 20 次采样累计 40ms（40_000_000 纳秒），单次均值应为 2ms（2_000_000 纳秒）。
        val record = runWithMetrics("r1", 1_000L, calls = 3, totalNanos = 40_000_000.0, samples = 20)
        val signals = record.cases.single().toSignals()

        assertEquals(2_000_000.0, signals.getValue("耗时·cangjie.analysis.resolve.phases.types.duration"), 1e-6)
        assertEquals(3.0, signals.getValue("计数·cangjie.analysis.analysisSessions.analyze.invocations"), 1e-9)
        assertEquals(1_000_000.0, signals.getValue(WALL_CLOCK_SIGNAL), 1e-9)
    }

    @Test
    fun timingSignalsBelowTheAbsoluteFloorAreTreatedAsNoise() {
        // 基线是 0.3ms 这种噪声底上的值：本次翻倍到 0.6ms 也只能是噪声。
        val history = listOf(0.3e6, 0.31e6, 0.29e6).mapIndexed { index, nanos ->
            run("r$index", (index + 1) * 1_000L, nanos.toLong())
        }
        val current = run("now", 9_000L, 0.6e6.toLong())

        val signal = CaPerformanceBaselineComparator.compare(current, history).cases.single().signals.single()

        assertEquals(CaPerformanceVerdict.NOISE, signal.verdict)
        assertTrue(signal.relativeChange!! > 0.5, "相对变化确实很大，只是落在分辨率下限之下")
    }

    @Test
    fun countersAreNotSubjectToTheTimingResolutionFloor() {
        // 计数是精确值：基线 3 次变 5 次，即使绝对值很小也是真的变了。
        val history = listOf(3L, 3L, 3L).mapIndexed { index, calls ->
            runWithMetrics("r$index", (index + 1) * 1_000L, calls = calls, totalNanos = 40_000_000.0, samples = 20)
        }
        val current = runWithMetrics("now", 9_000L, calls = 5, totalNanos = 40_000_000.0, samples = 20)

        val signals = CaPerformanceBaselineComparator.compare(current, history).cases.single().signals
        val counterSignal = signals.single {
            it.signalName == "计数·cangjie.analysis.analysisSessions.analyze.invocations"
        }

        assertEquals(CaPerformanceVerdict.REGRESSED, counterSignal.verdict)
    }

    @Test
    fun snapshotDeltaDropsUnchangedMetricsButKeepsDisappearingOnes() {
        val before = CaPerformanceSnapshot(
            counters = mapOf("a" to 5L, "b" to 7L),
            durationNanosSum = mapOf("h" to 1.0),
            histogramCounts = mapOf("h" to 1L),
        )
        val after = CaPerformanceSnapshot(
            counters = mapOf("a" to 8L, "b" to 7L),
            durationNanosSum = mapOf("h" to 3.0),
            histogramCounts = mapOf("h" to 2L),
        )

        val delta = after.minus(before)

        assertEquals(mapOf("a" to 3L), delta.counters)
        assertEquals(mapOf("h" to 2.0), delta.durationNanosSum)
        assertEquals(mapOf("h" to 1L), delta.histogramCounts)
    }

    @Test
    fun snapshotDeltaCountsAMetricThatDisappearedAsNegative() {
        // 本次没有、历史有：必须留下负增量，否则计数回落会被静默丢掉。
        val before = CaPerformanceSnapshot(mapOf("a" to 5L), emptyMap(), emptyMap())
        val after = CaPerformanceSnapshot(emptyMap(), emptyMap(), emptyMap())

        assertEquals(mapOf("a" to -5L), after.minus(before).counters)
    }

    @Test
    fun unitConversionFollowsTheSdkDeclaredUnit() {
        assertEquals(1_000_000.0, CaPerformanceSnapshot.toNanos("ms"), 1e-9)
        assertEquals(1_000.0, CaPerformanceSnapshot.toNanos("us"), 1e-9)
        assertEquals(1.0, CaPerformanceSnapshot.toNanos("ns"), 1e-9)
        assertEquals(1_000_000_000.0, CaPerformanceSnapshot.toNanos("s"), 1e-3)
        // 未声明单位按毫秒兜底：只会让报告显示得偏大，不会把结论判反。
        assertEquals(1_000_000.0, CaPerformanceSnapshot.toNanos(null), 1e-9)
    }

    @Test
    fun runRecordSurvivesJsonRoundTrip() {
        val original = runWithMetrics("r1", 1_000L, calls = 3, totalNanos = 40_000_000.0, samples = 20)
            .copy(gitCommit = "abc123")

        val parsed = CaPerformanceRunRecord.parse(original.toJsonLine())

        assertEquals(original.runId, parsed?.runId)
        assertEquals("abc123", parsed?.gitCommit)
        assertEquals(1_000L, parsed?.timestampMillis)
        assertEquals(
            original.cases.single().delta.counters,
            parsed?.casesById?.get("SampleTest#someCase")?.delta?.counters,
        )
    }

    @Test
    fun malformedHistoryLineIsSkippedInsteadOfFailing() {
        assertEquals(null, CaPerformanceRunRecord.parse("not json at all"))
        assertEquals(null, CaPerformanceRunRecord.parse("""{"runId":"x"}"""))
    }
}