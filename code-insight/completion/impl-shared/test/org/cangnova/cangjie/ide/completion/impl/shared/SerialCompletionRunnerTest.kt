package org.cangnova.cangjie.ide.completion.impl.shared

import com.intellij.openapi.progress.EmptyProgressIndicatorBase
import org.cangnova.cangjie.ide.completion.api.CompletionItemKind
import org.cangnova.cangjie.ide.completion.api.CompletionKind
import org.cangnova.cangjie.ide.completion.api.CompletionRequest
import org.cangnova.cangjie.ide.completion.api.CompletionSnapshot
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionOrigin
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 串行执行器的阶段与取消回归。
 *
 * 执行器只做三件事：按优先级调度、分批推送、传播取消。这三件事出错的表现都很隐蔽——
 * 顺序错了用户只是觉得候选来得慢，取消没传播则表现为「继续输入后弹窗还卡一下」，
 * 都不会报错。因此逐条断言批次序列。
 */
class SerialCompletionRunnerTest : CjParsingTestCase(
    dataPath = "",
    fileExt = "cj",
    fileType = CangJieFileType.INSTANCE,
    CangJieParserDefinition(),
) {
    @BeforeEach
    fun setUpFixture() = setUp()

    @AfterEach
    fun tearDownFixture() = tearDown()

    /**
     * 快速阶段的批次必须先于延迟阶段——这是「先给用户能看的结果」的全部含义。
     */
    @Test
    fun fastPhaseBatchesArriveBeforeDelayedPhase() {
        val sink = RecordingSink()
        runner.runSerial(
            request(),
            listOf(
                FakeSection(id = "delayed", executionPriority = 10, isDelayed = true, emitted = listOf("索引候选")),
                FakeSection(id = "fast", executionPriority = 90, emitted = listOf("局部候选")),
            ),
            sink,
            NotCanceled,
        )
        assertEquals(
            listOf("局部候选", "索引候选"),
            sink.batches.flatten(),
            "延迟阶段必须排在快速阶段之后，与执行优先级数值无关",
        )
    }

    /**
     * 同一阶段内按执行优先级升序执行，同优先级保持注册顺序。
     */
    @Test
    fun sectionsRunByPriorityWithinAPhase() {
        val sink = RecordingSink()
        runner.runSerial(
            request(),
            listOf(
                FakeSection(id = "c", executionPriority = 30, emitted = listOf("c")),
                FakeSection(id = "a", executionPriority = 10, emitted = listOf("a")),
                FakeSection(id = "b", executionPriority = 10, emitted = listOf("b")),
            ),
            sink,
            NotCanceled,
        )
        assertEquals(listOf("a", "b", "c"), sink.batches.flatten(), "同优先级必须保持注册顺序")
    }

    /**
     * 快速阶段结束后被取消：延迟阶段整段跳过。
     *
     * 用户已经继续输入时，前半段结果早已送达并可直接展示；
     * 再去算索引候选纯属浪费，而且会把一次被取消的请求拖成一次慢请求。
     */
    @Test
    fun delayedPhaseIsSkippedWhenCanceledAfterFastPhase() {
        val sink = RecordingSink()
        val indicator = CancelableIndicator()
        val delayed = FakeSection(id = "delayed", executionPriority = 90, isDelayed = true, emitted = listOf("索引候选"))
        // 快速阶段跑完时模拟「用户继续输入」：下一阶段的取消检查必须立刻命中。
        val fast = FakeSection(id = "fast", executionPriority = 10, emitted = listOf("局部候选")) {
            indicator.canceled = true
        }

        val canceled = runCatching { runner.runSerial(request(), listOf(fast, delayed), sink, indicator) }

        assertTrue(canceled.isFailure, "取消必须以异常传播，不得被吞成空批次")
        assertEquals(listOf("局部候选"), sink.batches.flatten(), "延迟阶段不得执行")
        assertEquals(listOf("fast"), fast.executedIds, "快速阶段已经跑过")
        assertTrue(delayed.executedIds.isEmpty(), "延迟阶段一个都不能跑")
    }

    /**
     * 收尾只通知一次，且在全部阶段之后。
     *
     * 收尾信号决定 UI 何时停止等待；多发会让界面提前收起，少发会让它一直转圈。
     */
    @Test
    fun lastIsEmittedExactlyOnceAfterAllPhases() {
        val sink = RecordingSink()
        runner.runSerial(
            request(),
            listOf(
                FakeSection(id = "fast", executionPriority = 10, emitted = listOf("f")),
                FakeSection(id = "delayed", executionPriority = 20, isDelayed = true, emitted = listOf("d")),
            ),
            sink,
            NotCanceled,
        )
        assertEquals(1, sink.lastFlags.count { it }, "收尾信号必须恰好一次")
        assertTrue(sink.lastFlags.last(), "收尾必须是最后一个信号")
        assertEquals(listOf(false, false, true), sink.lastFlags)
    }

    /**
     * 没有区段时也要收尾：否则界面永远等不到结果。
     */
    @Test
    fun emptySectionListStillFinishes() {
        val sink = RecordingSink()
        runner.runSerial(request(), emptyList(), sink, NotCanceled)
        assertEquals(listOf(true), sink.lastFlags)
    }

    /**
     * 执行优先级不决定展示顺序：执行器不得重排区段推来的候选。
     */
    @Test
    fun runnerDoesNotReorderCandidatesWithinABatch() {
        val sink = RecordingSink()
        val section = FakeSection(id = "fast", executionPriority = 10, emitted = listOf("z", "a", "m"))
        runner.runSerial(request(), listOf(section), sink, NotCanceled)
        // 执行器最后会再发一个空的收尾批次，因此取第一个。
        assertEquals(listOf("z", "a", "m"), sink.batches.first(), "执行器不得按名字或权重重排批内顺序")
    }

    /**
     * 执行器只在区段之间检查取消；单个区段内部的取消由区段自己负责。
     */
    private val runner = SerialCompletionRunner()

    /**
     * 构造一个只用于传递的补全请求；执行器不读它的内容。
     */
    private fun request(): CompletionRequest {
        val file = createPsiFile("runner", "func f(): Int64 { return 0 }") as CjFile
        val position = file.declarations.first() as CjElement
        return object : CompletionRequest {
            override val prefix: String = ""
            override val kind: CompletionKind = CompletionKind.BASIC
            override val invocationCount: Int = 1
            override val offset: Int = position.textOffset
            override val replaceStartOffset: Int = position.textOffset
            override val replaceEndOffset: Int = position.textOffset + position.textLength
            override val documentStamp: Long = 0L
            override val originalPosition: CjElement = position
            override val completionPosition: CjElement = position
        }
    }

    /**
     * 记录批次与收尾标记的 sink。
     */
    private class RecordingSink : CompletionResultSink {
        val batches = mutableListOf<List<String>>()
        val lastFlags = mutableListOf<Boolean>()

        override fun consume(batch: List<CompletionSnapshot>, isLast: Boolean) {
            batches += batch.map { it.lookupString }
            lastFlags += isLast
        }
    }

    /**
     * 记录自己的执行并把指定名字作为一批推出。
     */
    private class FakeSection(
        override val id: String,
        override val executionPriority: Int,
        override val isDelayed: Boolean = false,
        private val emitted: List<String>,
        private val onExecute: (() -> Unit)? = null,
    ) : CompletionSection {
        val executedIds = mutableListOf<String>()

        override fun execute(
            request: CompletionRequest,
            sink: CompletionResultSink,
            indicator: com.intellij.openapi.progress.ProgressIndicator,
        ) {
            executedIds += id
            onExecute?.invoke()
            sink.consume(emitted.map { name ->
                org.cangnova.cangjie.ide.completion.impl.shared.completionSnapshot(
                    lookupString = name,
                    kind = CompletionItemKind.TOP_LEVEL,
                    origin = CompletionOrigin.TOP_LEVEL,
                ).build()
            }, isLast = false)
        }
    }

    /**
     * 永不取消的指示器。
     */
    private object NotCanceled : EmptyProgressIndicatorBase() {
        override fun isCanceled(): Boolean = false
        override fun cancel() = Unit
        override fun start() = Unit
        override fun stop() = Unit
        override fun isRunning(): Boolean = true
    }

    /**
     * 可外部置为「已取消」的指示器。
     *
     * 只覆写 `isCanceled()`：基类把 `checkCanceled()` 定成 final，
     * 而执行器正是通过 `checkCanceled()` 观察取消的——这恰好验证了两者一致。
     */
    private class CancelableIndicator : EmptyProgressIndicatorBase() {
        var canceled = false

        override fun isCanceled(): Boolean = canceled
        override fun cancel() {
            canceled = true
        }

        override fun start() = Unit
        override fun stop() = Unit
        override fun isRunning(): Boolean = true
    }
}
