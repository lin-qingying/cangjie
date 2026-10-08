package org.cangnova.cangjie.lsp.server

import org.eclipse.lsp4j.jsonrpc.ResponseErrorException
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 一次 LSP 请求的取消状态。
 *
 * 存在的理由是**取消不会自动到达工作线程**：客户端发 `$/cancelRequest` 后，LSP4J 会对
 * 挂起的请求 future 调 `cancel(true)`（`RemoteEndpoint` 收到取消通知即如此），
 * 而我们的语义工作跑在 `CangjieRequestExecutor` 自己的线程上——`supplyAsync` 的取消
 * **不中断**正在跑的 supplier。因此必须显式把取消状态传进管线，
 * 让 `ProgressManager.checkCanceled()` 有机会抛出。
 *
 * 不依赖线程中断是有意的：执行器是**共享单线程**队列，残留的中断状态会污染下一个请求
 * （下一个请求一 `checkCanceled()` 就莫名取消）。
 */
fun interface LspRequestCancellation {
    /**
     * 当前请求是否已被取消。
     */
    fun isCancelled(): Boolean

    /**
     * 已取消则抛出 `RequestCancelled` 的服务端错误。
     *
     * 取消闸门：本容器里 `ProgressManager.checkCanceled()` **不生效**（实测：推入一个已取消的
     * 进度指示器，`checkCanceled()` 也不抛；因为容器的 `ProgressIndicatorProvider` 未接线，
     * 全局指示器恒为 null）。因此至少在管线**前后**各查一次——取消后不返回结果，
     * 用户不会拿到自己已经不要的候选。
     */
    fun throwIfCancelled() {
        if (isCancelled()) {
            throw ResponseErrorException(
                ResponseError(ResponseErrorCode.RequestCancelled, "补全已取消", null),
            )
        }
    }

    companion object {
        /**
         * 不可取消的实例：没有请求上下文时（测试、非请求路径）使用。
         */
        val NONE: LspRequestCancellation = LspRequestCancellation { false }
    }
}

/**
 * 由请求收到 `/cancelRequest` 时翻转的状态。
 */
class CancellableRequest : LspRequestCancellation {
    /**
     * 取消标志；`isCancelled` 会被管线在热路径上反复读取。
     */
    private val cancelled = AtomicBoolean(false)

    override fun isCancelled(): Boolean = cancelled.get()

    /**
     * 标记取消。可重复调用（取消通知可能重复到达）。
     */
    fun cancel() {
        cancelled.set(true)
    }
}
