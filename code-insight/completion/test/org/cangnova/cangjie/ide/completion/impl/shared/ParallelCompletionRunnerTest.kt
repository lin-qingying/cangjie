package org.cangnova.cangjie.ide.completion.impl.shared

import com.intellij.openapi.progress.EmptyProgressIndicatorBase
import com.intellij.openapi.progress.ProgressIndicator
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
import java.util.Collections
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

/**
 * 并行执行器与**串并行等价**回归（任务 13.3 的实现、13.4 的准入项）。
 *
 * 并行的风险不在「跑不跑得起来」，而在三件事静默地变样：
 * ①候选次序随调度抖动；②取消/失败时给出的批次与串行不一样；③区段之间共享了不该共享的状态。
 * 三条都不会报错，只会让用户看到「同样位置两次补全结果不同」。因此本类逐条对照串行结果断言。
 */
class ParallelCompletionRunnerTest : CjParsingTestCase(
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
     * 区段确实跑在不同线程上，且不是调用线程。
     *
     * 「并行」是实现手段不是目的，但若区段还在调用线程上跑，那就只是把串行包装成了并行。
     */
    @Test
    fun sectionsRunOnWorkerThreads() {
        val threads = Collections.synchronizedList(mutableListOf<Long>())
        val callerThread = Thread.currentThread().id
        val sections = listOf(
            FakeSection(id = "a", executionPriority = 10, emitted = listOf("a")) { threads += Thread.currentThread().id },
            FakeSection(id = "b", executionPriority = 20, emitted = listOf("b")) { threads += Thread.currentThread().id },
        )
        // 显式给足两个线程：并行度本身取 min(区段数, 核数, 上限)，单核机器上默认只有 1。
        val runner = ParallelCompletionRunner(maxThreads = 2)

        runner.runParallel(request(), sections, RecordingSink(), NotCanceled)

        assertEquals(2, threads.size, "两个区段都必须执行")
        assertTrue(threads.none { it == callerThread }, "区段不得在调用线程上执行，实际 $threads")
        assertEquals(2, threads.toSet().size, "两个区段应各占一个工作线程，实际 $threads")
    }

    /**
     * 后面的区段先跑完，批次仍必须按区段次序发出。
     *
     * 这条是「稳定归并」的全部含义：按完成先后发批次会让候选次序随调度抖动。
     */
    @Test
    fun laterSectionFinishingFirstStillEmitsAfterEarlierSection() {
        val completionOrder = Collections.synchronizedList(mutableListOf<String>())
        val slowFirst = FakeSection(id = "first", executionPriority = 10, emitted = listOf("慢区段")) {
            Thread.sleep(50)
            completionOrder += "first"
        }
        val fastSecond = FakeSection(id = "second", executionPriority = 20, emitted = listOf("快区段")) {
            completionOrder += "second"
        }
        val sink = RecordingSink()

        parallelRunner.runParallel(request(), listOf(slowFirst, fastSecond), sink, NotCanceled)

        assertEquals(listOf("second", "first"), completionOrder, "前提：快的区段确实先跑完")
        assertEquals(
            listOf("慢区段", "快区段"),
            sink.batches.flatten(),
            "归并次序必须是区段次序，不是完成次序",
        )
    }

    /**
     * 同一组区段（含延迟阶段、同优先级、多批次）在两种执行器下产生**完全一致**的批次序列。
     */
    @Test
    fun parallelBatchesEqualSerialBatches() {
        fun sections() = listOf(
            FakeSection(id = "delayed", executionPriority = 10, isDelayed = true, emitted = listOf("索引")),
            FakeSection(id = "third", executionPriority = 90, emitted = listOf("顶层二")),
            FakeSection(id = "first", executionPriority = 50, emitted = listOf("局部")),
            FakeSection(id = "second", executionPriority = 90, emitted = listOf("顶层一")),
        )
        val serialSink = RecordingSink()
        val parallelSink = RecordingSink()

        SerialCompletionRunner().runSerial(request(), sections(), serialSink, NotCanceled)
        parallelRunner.runParallel(request(), sections(), parallelSink, NotCanceled)

        assertEquals(serialSink.batches, parallelSink.batches, "串并行候选批次必须一致（含阶段划分与同优先级次序）")
        assertEquals(serialSink.lastFlags, parallelSink.lastFlags, "收尾信号序列也必须一致")
    }

    /**
     * 区段之间不共享 sink：并行时每个区段拿到自己的缓冲 sink。
     */
    @Test
    fun sectionsDoNotShareSinks() {
        val seenSinks = Collections.synchronizedList(mutableListOf<CompletionResultSink>())
        val sections = listOf(
            RecordingSectionSink(id = "a", executionPriority = 10, seenSinks = seenSinks),
            RecordingSectionSink(id = "b", executionPriority = 20, seenSinks = seenSinks),
        )

        parallelRunner.runParallel(request(), sections, RecordingSink(), NotCanceled)

        assertEquals(2, seenSinks.size, "两个区段都必须执行")
        assertNotSame(seenSinks[0], seenSinks[1], "区段不得共用同一个 sink（共用即等于共享可变状态）")
    }

    /**
     * 早序区段的失败按原样抛出，且**后续区段的批次不得发出**——与串行语义一致。
     */
    @Test
    fun failurePreventsLaterBatchesAndPropagates() {
        val failure = IllegalStateException("区段失败")
        val first = FakeSection(id = "first", executionPriority = 10, emitted = listOf("第一批"))
        val failing = FakeSection(id = "failing", executionPriority = 20, emitted = listOf("不该出现")) {
            throw failure
        }
        val later = FakeSection(id = "later", executionPriority = 30, emitted = listOf("更不该出现"))
        val sink = RecordingSink()

        val thrown = runCatching { parallelRunner.runParallel(request(), listOf(first, failing, later), sink, NotCanceled) }

        assertTrue(thrown.exceptionOrNull() === failure, "区段异常必须按原样传播，实际 ${thrown.exceptionOrNull()}")
        assertEquals(listOf("第一批"), sink.batches.flatten(), "失败之后的批次一律不得发出")
        assertEquals(listOf(false), sink.lastFlags, "失败时只应看到第一批的非收尾信号，不得发正常收尾")
    }

    /**
     * 前一个区段之后用户继续输入：已完成的批次照常发出，后续区段整段跳过——与串行一致。
     */
    @Test
    fun cancellationAfterFirstSectionMatchesSerialSemantics() {
        fun sections(indicator: CancelableIndicator) = listOf(
            FakeSection(id = "first", executionPriority = 10, emitted = listOf("第一批")) {
                indicator.canceled = true
            },
            FakeSection(id = "second", executionPriority = 20, emitted = listOf("第二批")),
        )

        val serialIndicator = CancelableIndicator()
        val serialSink = RecordingSink()
        val serialResult = runCatching {
            SerialCompletionRunner().runSerial(request(), sections(serialIndicator), serialSink, serialIndicator)
        }

        val parallelIndicator = CancelableIndicator()
        val parallelSink = RecordingSink()
        val parallelResult = runCatching {
            parallelRunner.runParallel(request(), sections(parallelIndicator), parallelSink, parallelIndicator)
        }

        assertTrue(serialResult.isFailure && parallelResult.isFailure, "两者都必须以取消异常结束")
        assertEquals(serialSink.batches, parallelSink.batches, "取消时给出的批次也必须与串行一致")
        assertEquals(serialSink.lastFlags, parallelSink.lastFlags, "取消时都不得发收尾信号")
    }

    /**
     * 没有区段时仍要收尾，且只收尾一次。
     */
    @Test
    fun emptySectionListStillFinishes() {
        val sink = RecordingSink()
        parallelRunner.runParallel(request(), emptyList(), sink, NotCanceled)
        assertEquals(listOf(true), sink.lastFlags)
    }

    /**
     * 并行执行器仍支持串行入口（它只是同一个接口的另一个实现，不替换串行路径）。
     */
    @Test
    fun parallelRunnerStillRunsSeriallyOnRequest() {
        val sink = RecordingSink()
        parallelRunner.runSerial(
            request(),
            listOf(FakeSection(id = "only", executionPriority = 10, emitted = listOf("唯一"))),
            sink,
            NotCanceled,
        )
        assertEquals(listOf("唯一"), sink.batches.flatten())
        assertEquals(listOf(false, true), sink.lastFlags, "批次非收尾 + 最后的收尾信号")
    }

    /**
     * 使用的执行器实例。
     */
    private val parallelRunner = ParallelCompletionRunner()

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
        val batches = Collections.synchronizedList(mutableListOf<List<String>>())
        val lastFlags = Collections.synchronizedList(mutableListOf<Boolean>())

        override fun consume(batch: List<CompletionSnapshot>, isLast: Boolean) {
            batches += batch.map { it.lookupString }
            lastFlags += isLast
        }
    }

    /**
     * 记录自己拿到的 sink 的区段（用于验证「区段之间不共享 sink」）。
     */
    private class RecordingSectionSink(
        override val id: String,
        override val executionPriority: Int,
        private val seenSinks: MutableList<CompletionResultSink>,
    ) : CompletionSection {
        override fun execute(request: CompletionRequest, sink: CompletionResultSink, indicator: ProgressIndicator) {
            seenSinks += sink
        }
    }

    /**
     * 记录自己的执行并把指定名字作为一批推出。
     */
    private class FakeSection(
        override val id: String,
        override val executionPriority: Int,
        override val isDelayed: Boolean = false,
        private val emitted: List<String> = emptyList(),
        private val onExecute: (() -> Unit)? = null,
    ) : CompletionSection {
        override fun execute(request: CompletionRequest, sink: CompletionResultSink, indicator: ProgressIndicator) {
            onExecute?.invoke()
            if (emitted.isEmpty()) return
            sink.consume(emitted.map { name ->
                completionSnapshot(
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
     * 可外部置为「已取消」的指示器；只覆写 `isCanceled()`，与串行用例保持同一形态。
     */
    private class CancelableIndicator : EmptyProgressIndicatorBase() {
        @Volatile
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
