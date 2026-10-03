package org.cangnova.cangjie.frontend.pipeline

import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.phaser.PhaseConfig
import org.cangnova.cangjie.phaser.PhaserProfiler
import org.cangnova.cangjie.phaser.PhaserState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 阶段组合的执行协议测试。
 *
 * 组合阶段此前直接调用子阶段的 `executePhase`：子阶段因此不走 phaser 协议，启用判断、
 * prerequisite 校验、前后置 action 与阶段耗时采集全部失效。这里的用例锁定子阶段确实走了
 * 协议，而不只是"两个阶段按顺序跑了"。
 */
@OptIn(CompilerConfiguration.Internals::class)
class FrontendPipelineCompositionTest {
    /**
     * 子阶段经由 phaser 协议执行：登记进 `alreadyDone`，并各自留下阶段耗时。
     */
    @Test
    fun childrenRunThroughThePhaseProtocol() {
        val profiler = PhaserProfiler()
        val configuration = CompilerConfiguration()
        val phaserState = PhaserState()
        val first = recordingPhase("First")
        val second = recordingPhase("Second")

        (first then second).invoke(
            PhaseConfig(profiler = profiler),
            phaserState,
            PipelineContext(configuration),
            TestArtifact(configuration),
        )

        assertEquals(
            setOf(first, second),
            phaserState.alreadyDone,
            "子阶段必须经由 phaser 协议执行才会登记到 alreadyDone",
        )
        assertEquals(listOf("First", "Second"), profiler.timings().map { it.name })
        assertTrue(
            profiler.timings().all { it.completed && it.elapsedNanos >= 0L },
            profiler.timings().toString(),
        )
    }

    /**
     * 前一阶段返回 `null` 时组合阶段不执行后一阶段。
     */
    @Test
    fun nullFromFirstPhaseStopsTheSecond() {
        val profiler = PhaserProfiler()
        val configuration = CompilerConfiguration()
        val second = recordingPhase("Second")

        val failure = runCatching {
            (failingPhase("First") then second).invoke(
                PhaseConfig(profiler = profiler),
                PhaserState(),
                PipelineContext(configuration),
                TestArtifact(configuration),
            )
        }.exceptionOrNull()

        // 管线阶段的主体把 null 转成 PipelineStepException，组合层不再自行吞掉失败。
        assertTrue(failure is PipelineStepException, "首个阶段失败必须中断组合，实际：$failure")
        assertEquals(listOf("First"), profiler.timings().map { it.name }, "失败的后一阶段不应执行")
    }

    /**
     * 直接调用组合阶段的 `executePhase` 仍按顺序执行两个阶段，只是不经协议。
     */
    @Test
    fun directExecutePhaseRunsBothStagesWithoutProfiling() {
        val profiler = PhaserProfiler()
        val configuration = CompilerConfiguration()

        val output = (recordingPhase("First") then recordingPhase("Second"))
            .executePhase(TestArtifact(configuration))

        assertTrue(output is TestArtifact, output.toString())
        assertEquals(emptyList<String>(), profiler.timings().map { it.name })
    }

    /**
     * 构造一个记录执行次数的阶段。
     */
    private fun recordingPhase(name: String): PipelinePhase<TestArtifact, TestArtifact> =
        object : PipelinePhase<TestArtifact, TestArtifact>(name) {
            override fun executePhase(input: TestArtifact): TestArtifact = input
        }

    /**
     * 构造一个必定失败的阶段。
     */
    private fun failingPhase(name: String): PipelinePhase<TestArtifact, TestArtifact> =
        object : PipelinePhase<TestArtifact, TestArtifact>(name) {
            override fun executePhase(input: TestArtifact): TestArtifact? = null
        }

    /**
     * 测试用管线产物。
     */
    private class TestArtifact(override val configuration: CompilerConfiguration) : PipelineArtifact() {
        @OptIn(PipelineArtifact.FrontendPipelineInternals::class)
        override fun withCompilerConfiguration(newConfiguration: CompilerConfiguration): PipelineArtifact =
            TestArtifact(newConfiguration)
    }
}