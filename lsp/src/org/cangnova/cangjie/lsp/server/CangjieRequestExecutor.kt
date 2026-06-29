package org.cangnova.cangjie.lsp.server

import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Level
import java.util.logging.Logger

/**
 * 串行执行 LSP 请求的工作线程执行器。
 *
 * 该执行器把服务端能力计算统一收敛到单线程队列，避免 PSI、文档状态和 Analysis API 快照被并发请求交叉修改。
 *
 * @param threadNamePrefix 工作线程名称前缀，便于日志和线程 dump 识别 LSP 请求线程。
 */
class CangjieRequestExecutor(
    threadNamePrefix: String = "cangjie-lsp-worker",
) : AutoCloseable {
    /**
     * 请求执行器日志记录器。
     */
    private val logger = Logger.getLogger(CangjieRequestExecutor::class.java.name)

    /**
     * 为工作线程生成稳定递增编号的计数器。
     */
    private val threadCounter = AtomicInteger(1)

    /**
     * 实际承载 LSP 请求的单线程 executor。
     *
     * 线程设置为 daemon，确保宿主进程关闭时不会被后台请求线程阻塞。
     */
    private val executor: ExecutorService = Executors.newSingleThreadExecutor(
        ThreadFactory { runnable ->
            Thread(runnable, "$threadNamePrefix-${threadCounter.getAndIncrement()}").apply {
                isDaemon = true
            }
        },
    )

    /**
     * 在 LSP 工作线程上异步执行请求动作。
     *
     * 该方法将异常记录到日志后继续让 `CompletableFuture` 以失败状态完成，保持 JSON-RPC 错误传播链。
     */
    fun <T> compute(action: () -> T): CompletableFuture<T> {
        return CompletableFuture.supplyAsync(
            {
                try {
                    action()
                } catch (throwable: Throwable) {
                    logger.log(Level.SEVERE, "LSP request executor action failed", throwable)
                    throw throwable
                }
            },
            executor,
        )
    }

    /**
     * 关闭请求执行器并取消仍在队列中的任务。
     */
    override fun close() {
        executor.shutdownNow()
    }
}
