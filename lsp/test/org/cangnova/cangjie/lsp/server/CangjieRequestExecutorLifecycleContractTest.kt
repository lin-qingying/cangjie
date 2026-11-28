package org.cangnova.cangjie.lsp.server

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.CancellationException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** 直接使用真实产品执行器验证队列收尾，不替换 server/engine 或模拟语义结果。 */
@Timeout(10)
class CangjieRequestExecutorLifecycleContractTest {
    @Test
    fun `shutdown completes queued notification futures`() {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val executor = CangjieRequestExecutor()
        try {
            executor.compute(CangjieLspRequest.REFERENCES) {
                started.countDown()
                try {
                    release.await()
                } catch (_: InterruptedException) {
                    // 此动作仅控制真实 worker 的占用；关闭中断是该调度门的预期收尾。
                    Thread.currentThread().interrupt()
                }
            }
            assertTrue(started.await(5, TimeUnit.SECONDS))
            val queued = executor.executeNotification {
                throw AssertionError("关闭前排队且尚未开始的通知不得在关闭后执行")
            }
            executor.close()
            assertTrue(queued.isDone, "shutdownNow 排空任务后，通知等待方也必须完成")
            assertTrue(queued.isCancelled)
            assertThrows(CancellationException::class.java) { queued.join() }
            assertThrows(RejectedExecutionException::class.java) { executor.executeNotification {} }
        } finally {
            release.countDown()
            executor.close()
        }
    }

    @Test
    fun `notification diagnostics reentry completes on the same worker`() {
        val executions = AtomicInteger()
        CangjieRequestExecutor().use { executor ->
            val outer = executor.executeNotification {
                executor.compute(CangjieLspRequest.PUBLISH_DIAGNOSTICS) {
                    executions.incrementAndGet()
                }.join()
            }
            outer.get(5, TimeUnit.SECONDS)
            assertEquals(1, executions.get(), "通知中的正式诊断刷新不能排队等待自己")
        }
    }
}
