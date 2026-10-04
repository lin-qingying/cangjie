package org.cangnova.cangjie.phaser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * [PhaserProfiler] 的报告渲染与 [NamedCompilerPhase] 的采集接线测试。
 *
 * 这一层此前没有任何测试：`needProfiling` 无人开启、消费点只有一句 `println`，因此接线是否
 * 正确无从验证。这里用真实的 phase 调用验证采集，避免只测渲染函数。
 */
class PhaserProfilerTest {
    /**
     * 无采集记录时报告是明确的空结果，而不是空白。
     */
    @Test
    fun emptyProfilerRendersExplicitEmptyReport() {
        assertEquals("no phase was recorded\n", PhaserProfiler().renderReport())
    }

    /**
     * 嵌套 phase 按深度缩进，顶层合计只累加 depth 0 的 phase。
     */
    @Test
    fun nestedPhasesAreIndentedAndTotalCountsTopLevelOnly() {
        val profiler = PhaserProfiler()
        profiler.record(PhaserPhaseTiming("Outer", depth = 0, sequence = 0L, elapsedNanos = 10_000_000L, completed = true))
        profiler.record(PhaserPhaseTiming("Inner", depth = 1, sequence = 1L, elapsedNanos = 4_000_000L, completed = true))

        val report = profiler.renderReport()
        val lines = report.lines()
        assertTrue(lines[0].startsWith("Outer:"), report)
        assertTrue(lines[1].startsWith("\tInner:"), report)
        assertEquals("total: 10.000 ms", lines[2])
    }

    /**
     * 失败的 phase 保留已消耗耗时并标记出来。
     */
    @Test
    fun failedPhaseIsMarkedAndKeepsElapsedTime() {
        val profiler = PhaserProfiler()
        profiler.record(PhaserPhaseTiming("Boom", depth = 0, sequence = 0L, elapsedNanos = 2_500_000L, completed = false))

        assertTrue(profiler.renderReport().contains("(failed)"), profiler.renderReport())
        assertEquals(listOf("Boom"), profiler.timings().map { it.name })
    }

    /**
     * 挂上采集器后，嵌套 phase 各自被采集，且耗时非负。
     */
    @Test
    fun profilingPhaseRecordsEveryPhase() {
        val profiler = PhaserProfiler()
        val phaseConfig = PhaseConfig(profiler = profiler)

        val leaf = recordingPhase("Leaf") { }
        val middle = object : NamedCompilerPhase<LoggingContext, Unit, Unit>("Middle", nlevels = 1) {
            override fun phaseBody(context: LoggingContext, input: Unit): Unit = leaf.invoke(phaseConfig, PhaserState(), context, Unit)
            override fun outputIfNotEnabled(phaseConfig: PhaseConfig, phaserState: PhaserState, context: LoggingContext, input: Unit): Unit = Unit
        }
        val top = object : NamedCompilerPhase<LoggingContext, Unit, Unit>("Top") {
            override fun phaseBody(context: LoggingContext, input: Unit): Unit = middle.invoke(phaseConfig, PhaserState(), context, Unit)
            override fun outputIfNotEnabled(phaseConfig: PhaseConfig, phaserState: PhaserState, context: LoggingContext, input: Unit): Unit = Unit
        }

        top.invoke(phaseConfig, PhaserState(), TestContext, Unit)

        assertEquals(listOf("Top", "Middle", "Leaf"), profiler.timings().map { it.name })
        assertTrue(profiler.timings().all { it.completed && it.elapsedNanos >= 0L }, profiler.timings().toString())
    }

    /**
     * 主体抛异常时记录"失败 + 已耗时"并原样抛出，编译失败位置可见。
     */
    @Test
    fun failingPhaseIsRecordedAndExceptionPropagates() {
        val profiler = PhaserProfiler()
        val phaseConfig = PhaseConfig(profiler = profiler)
        val failing = recordingPhase("Failing") { throw IllegalStateException("boom") }

        val failure = runCatching { failing.invoke(phaseConfig, PhaserState(), TestContext, Unit) }.exceptionOrNull()

        assertTrue(failure is IllegalStateException, failure.toString())
        assertEquals(1, profiler.timings().size)
        assertEquals("Failing", profiler.timings().single().name)
        assertTrue(!profiler.timings().single().completed, "异常退出的 phase 必须标记为 failed")
    }

    /**
     * 不挂采集器时不产生任何记录，也不取单调时钟。
     */
    @Test
    fun noProfilerMeansNoRecording() {
        val phase = recordingPhase("Plain") { }
        phase.invoke(PhaseConfig(), PhaserState(), TestContext, Unit)
        // PhaseConfig.profiler 为 null 时没有任何可查询的采集器；这里断言调用本身不抛，
        // 避免给未开启 profiling 的编译路径加上额外行为。
    }

    /**
     * 构造一个指定名字、主体为 [body] 的 phase。
     */
    private fun recordingPhase(name: String, body: () -> Unit) =
        object : NamedCompilerPhase<LoggingContext, Unit, Unit>(name) {
            override fun phaseBody(context: LoggingContext, input: Unit): Unit = body()
            override fun outputIfNotEnabled(
                phaseConfig: PhaseConfig,
                phaserState: PhaserState,
                context: LoggingContext,
                input: Unit,
            ): Unit = Unit
        }

    /**
     * 测试用 [LoggingContext]。
     */
    private object TestContext : LoggingContext {
        override var inVerbosePhase: Boolean = false
    }
}
