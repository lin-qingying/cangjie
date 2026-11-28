package org.cangnova.cangjie.lsp.server

import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.time.TimeSource

/**
 * 串行执行 LSP 请求的工作线程执行器。
 *
 * 该执行器把服务端能力计算统一收敛到单线程队列，避免 PSI、文档状态和 Analysis API 快照被并发请求交叉修改。
 *
 * @param threadNamePrefix 工作线程名称前缀，便于日志和线程 dump 识别 LSP 请求线程。
 * @param statistics 请求耗时统计域；统计未启用时为 `null`，此时不取单调时钟。
 */
class CangjieRequestExecutor(
    threadNamePrefix: String = "cangjie-lsp-worker",
    private val statistics: CangjieRequestStatistics? = null,
) : AutoCloseable {
    /**
     * 请求执行器日志记录器。
     */
    private val logger = Logger.getLogger(CangjieRequestExecutor::class.java.name)

    /**
     * 为工作线程生成稳定递增编号的计数器。
     */
    private val threadCounter = AtomicInteger(1)

    /** 同一 worker 上的重入计算必须直接完成，避免通知中的诊断刷新 join 等待自身。 */
    @Volatile
    private var workerThread: Thread? = null

    /** 提交与关闭共用生命周期锁；锁内仅登记任务，取消回调在锁外完成。 */
    private val lifecycleLock = Any()
    private val pendingFutures = linkedSetOf<CompletableFuture<*>>()

    /** 单线程队列同时承载请求与通知；daemon 不阻止宿主退出。 */
    private val executor: ExecutorService = Executors.newSingleThreadExecutor(
        ThreadFactory { runnable ->
            Thread(runnable, "$threadNamePrefix-${threadCounter.getAndIncrement()}").apply {
                isDaemon = true
                workerThread = this
            }
        },
    )

    /**
     * 在 LSP 工作线程上异步执行 [request] 对应的动作。
     *
     * 该方法将异常记录到日志后继续让 `CompletableFuture` 以失败状态完成，保持 JSON-RPC 错误传播链；
     * 失败同时计入请求统计的失败计数，并让请求链路 span 以 ERROR 状态结束。耗时不包含排队等待——
     * 排队时长属于服务器背压，与请求本身的成本是两件事，混在一起会让补全变慢时误以为是分析变慢。
     */
    fun <T> compute(request: CangjieLspRequest, action: () -> T): CompletableFuture<T> =
        enqueue { measure(request, action) }

    /** 文档/工作区通知与请求共享同一队列；通知不伪装成某种语义请求统计。 */
    fun executeNotification(action: () -> Unit): CompletableFuture<Unit> = enqueue(action)

    private fun <T> enqueue(action: () -> T): CompletableFuture<T> {
        if (Thread.currentThread() !== workerThread || executor.isShutdown) {
            val future = synchronized(lifecycleLock) {
                CompletableFuture.supplyAsync({ action() }, executor).also { pendingFutures.add(it) }
            }
            future.whenComplete { _, _ ->
                synchronized(lifecycleLock) { pendingFutures.remove(future) }
            }
            return future
        }
        val result = CompletableFuture<T>()
        try {
            result.complete(action())
        } catch (failure: Throwable) {
            result.completeExceptionally(failure)
        }
        return result
    }

    /** 保留原请求 span、失败日志和完成统计；重入刷新也在当前 worker 上正确配对。 */
    private fun <T> measure(request: CangjieLspRequest, action: () -> T): T {
        val requestSpan = statistics?.startRequestSpan(request)
        val startedAt = if (statistics != null) TimeSource.Monotonic.markNow() else null
        var failed = false
        var error: Throwable? = null
        try {
            return action()
        } catch (throwable: Throwable) {
            failed = true
            error = throwable
            logger.log(Level.SEVERE, "LSP request ${request.lspMethod} failed", throwable)
            throw throwable
        } finally {
            if (startedAt != null) {
                statistics!!.onRequestFinished(
                    request,
                    startedAt.elapsedNow().inWholeNanoseconds,
                    failed,
                    error,
                    requestSpan,
                )
            }
        }
    }

    /**
     * 关闭请求执行器并取消仍在队列中的任务。
     */
    override fun close() {
        val pending = synchronized(lifecycleLock) {
            executor.shutdownNow()
            pendingFutures.toList().also { pendingFutures.clear() }
        }
        // shutdownNow 只返回未运行任务，不能完成 supplyAsync 的 future；所有等待方必须收到取消。
        pending.forEach { it.cancel(true) }
    }
}
