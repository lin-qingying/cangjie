package org.cangnova.cangjie.ide.completion.impl.shared

import com.intellij.openapi.progress.ProgressIndicator
import org.cangnova.cangjie.ide.completion.api.CompletionRequest
import org.cangnova.cangjie.ide.completion.api.CompletionSnapshot
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

/**
 * 并行执行器（任务 13.3）。
 *
 * 与 [SerialCompletionRunner] 的关系是**结果等价、执行并行**：
 * - 阶段划分（快速 → 延迟）与阶段内次序（`executionPriority` 升序，同优先级保持注册顺序）逐条照搬；
 * - 每个区段在**自己的线程**上执行，用**自己的缓冲 sink** 收批次；
 * - 归并由执行器在调用线程上按上述次序完成：第 i 个区段一结束，就立刻把它的批次推给真实 sink。
 *
 * 三条硬约束（缺一不可，对应 13.4 的准入项）：
 * 1. **会话隔离**：区段之间不共享缓冲 sink（各拿各的），也不得共享裸 Analysis 会话——
 *    这条靠区段的并发契约（见 [CompletionSection] 的 KDoc）保证，执行器只保证不主动引入共享状态；
 * 2. **稳定归并**：真实 sink 只被调用线程访问，且批次次序与串行完全一致——
 *    即便后面的区段先跑完，它的批次也要等前面的区段归并完才发出；
 * 3. **取消与失败按序传播**：按执行次序等待，因此**最早**的失败/取消就是向外抛出的那一个；
 *    失败或取消后已提交的区段被中断，未归并的批次一律丢弃，且不发收尾信号。
 *
 * **默认不启用**：真实工程上的耗时与隔离准入（13.4）未达成前，装配点只创建串行执行器
 * （见 [CangJieCompletionRunnerOptions]）。
 */
class ParallelCompletionRunner(
    /**
     * 并行度上限。
     *
     * 区段数量通常个位数，且区段内部是分析调用（CPU 密集），因此取「区段数」与
     * 处理器核数、[MAX_THREADS] 三者的最小值，避免开出一池空转线程。
     */
    private val maxThreads: Int = minOf(Runtime.getRuntime().availableProcessors(), MAX_THREADS),
) : CompletionRunner {

    /**
     * 该执行器不改变串行路径：显式调用串行入口时走 [SerialCompletionRunner]。
     */
    override fun runSerial(
        request: CompletionRequest,
        sections: List<CompletionSection>,
        sink: CompletionResultSink,
        indicator: ProgressIndicator,
    ) {
        SerialCompletionRunner().runSerial(request, sections, sink, indicator)
    }

    /**
     * 并行执行一个请求的各区段，按串行次序归并批次。
     */
    override fun runParallel(
        request: CompletionRequest,
        sections: List<CompletionSection>,
        sink: CompletionResultSink,
        indicator: ProgressIndicator,
    ) {
        val ordered = sections.sortedBy { it.executionPriority }
        val (fast, delayed) = ordered.partition { !it.isDelayed }
        runPhase(fast, request, sink, indicator)
        // 延迟阶段进入前重新检查取消：与串行同义，别把一次被取消的请求拖成慢请求。
        runPhase(delayed, request, sink, indicator)
        sink.consume(emptyList(), isLast = true)
    }

    /**
     * 并行执行一个阶段，并在调用线程上按执行次序归并。
     */
    private fun runPhase(
        sections: List<CompletionSection>,
        request: CompletionRequest,
        sink: CompletionResultSink,
        indicator: ProgressIndicator,
    ) {
        if (sections.isEmpty()) return
        indicator.checkCanceled()

        val pool = Executors.newFixedThreadPool(
            minOf(sections.size, maxThreads).coerceAtLeast(1),
            SectionThreadFactory,
        )
        try {
            // 每区段一个缓冲 sink：区段之间看不到彼此的批次，归并前真实 sink 只由调用线程访问。
            val buffers = sections.associateWith { BufferingSink() }
            val futures = sections.map { section ->
                pool.submit {
                    section.execute(request, buffers.getValue(section), indicator)
                }
            }

            // 按执行次序归并：第 i 个区段一完成就发它的批次。**不**按完成先后发，
            // 否则候选次序会随调度抖动，与串行结果不再等价。
            for ((index, section) in sections.withIndex()) {
                awaitSection(futures[index], section)
                buffers.getValue(section).flushTo(sink)
                // 取消检查放在**归并之后**：串行路径在区段一执行完就已经把批次交给了 sink，
                // 那时用户继续输入不会收回已发结果；并行必须保持同一语义，否则同样的取消
                // 时机会让并行少给一批候选。
                indicator.checkCanceled()
            }
        } finally {
            // 失败/取消路径：中断仍在跑的区段。不等它退出——区段自己按 indicator 检查取消，
            // 阻塞等待只会把一次失败的补全拖成一次慢失败。
            pool.shutdownNow()
        }
    }

    /**
     * 等待一个区段结束，把它的失败按原样抛出。
     */
    private fun awaitSection(
        future: java.util.concurrent.Future<*>,
        section: CompletionSection,
    ) {
        try {
            future.get()
        } catch (e: ExecutionException) {
            // 区段自己的异常（含取消）原样向上：包装会让调用方分不出「取消」与「失败」。
            throw e.cause ?: e
        } catch (e: CancellationException) {
            throw e
        }
    }

    /**
     * 单个区段的批次缓冲。
     *
     * 写入端是区段线程，读取端是归并线程；批次列表自身同步，`flushTo` 只在归并线程调用。
     */
    private class BufferingSink : CompletionResultSink {
        private val batches = mutableListOf<List<CompletionSnapshot>>()

        override fun consume(batch: List<CompletionSnapshot>, isLast: Boolean) {
            if (isLast) return
            synchronized(batches) {
                batches += batch
            }
        }

        /** 按接收顺序把缓冲批次推给真实 sink。 */
        fun flushTo(sink: CompletionResultSink) {
            val pending = synchronized(batches) {
                batches.toList().also { batches.clear() }
            }
            pending.forEach { sink.consume(it, isLast = false) }
        }
    }

    /**
     * 区段线程工厂：命名便于排查，daemon 避免阻塞 JVM 退出。
     */
    private object SectionThreadFactory : ThreadFactory {
        private var counter = 0

        override fun newThread(runnable: Runnable): Thread =
            Thread(runnable, "cangjie-completion-section-${++counter}").apply { isDaemon = true }
    }

    private companion object {
        /** 并行度上限：区段是 CPU 密集型，超过这个数只是多占线程。 */
        const val MAX_THREADS = 4
    }
}

/**
 * 执行器装配点。
 *
 * 并行执行器**默认关闭**：13.4 的准入项（串并行候选/排序等价、取消与异常一致、增量失效一致、
 * 真实工程耗时对比）未达成前不得开启。宿主可在设置层改 [parallelEnabled]；
 * 系统属性 `cangjie.completion.parallel` 作为免设置的实验入口（性能对比与测试用）。
 */
object CangJieCompletionRunnerOptions {
    /** 实验入口的系统属性名。 */
    const val PARALLEL_RUNNER_PROPERTY: String = "cangjie.completion.parallel"

    /**
     * 是否启用并行执行器。
     *
     * 初始化时读一次系统属性；宿主设置层可随时改写（[Volatile] 保证可见性）。
     */
    @Volatile
    var parallelEnabled: Boolean = java.lang.Boolean.getBoolean(PARALLEL_RUNNER_PROPERTY)
}

/**
 * 按当前选项创建执行器；装配点只调用本函数，不直接 new 具体执行器。
 */
fun createCompletionRunner(): CompletionRunner =
    if (CangJieCompletionRunnerOptions.parallelEnabled) ParallelCompletionRunner() else SerialCompletionRunner()
