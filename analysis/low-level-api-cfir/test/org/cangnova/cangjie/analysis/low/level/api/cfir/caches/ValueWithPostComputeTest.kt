package org.cangnova.cangjie.analysis.low.level.api.cfir.caches

import com.intellij.openapi.progress.ProcessCanceledException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

/**
 * [ValueWithPostCompute] 的并发正确性测试。
 *
 * 覆盖三个核心契约：
 * 1. 多线程并发访问时值只计算一次，且所有线程拿到同一结果；
 * 2. postCompute 抛出的可恢复异常（PCE）不被缓存，且每次访问都会重新抛出；
 * 3. 持有锁的线程在 postCompute 阶段抛出 PCE 后，等待中的线程必须能重新计算而不是拿到半成品。
 *
 * 对齐 Kotlin `ValueWithPostComputeTest`。
 */
class ValueWithPostComputeTest {
    /**
     * 多线程并发访问，值应只计算一次且结果一致。
     */
    @Test
    fun testTheSameValueIsComputedFromDifferentThreads() {
        val valueWithPostCompute = ValueWithPostCompute(
            key = 1,
            calculate = { Thread.currentThread().name to Unit },
            postCompute = { _, _, _ -> },
        )

        val results = ConcurrentLinkedQueue<Any?>()

        val threads = (0..9).map { threadIndex ->
            thread(name = "t$threadIndex", start = false) {
                results.offer(valueWithPostCompute.getValue())
            }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }

        val resultsList = results.toList()
        Assertions.assertEquals(threads.size, results.size)
        Assertions.assertTrue(
            resultsList.all { it == resultsList[0] },
            "All results got from ValueWithPostCompute should be equal, but was $resultsList",
        )
    }

    /**
     * postCompute 抛出的 PCE 不应被缓存，每次访问都应重新抛出新的异常。
     */
    @Test
    fun testPCEIsRethrownAndNotSavedInCache() {
        val valueWithPostCompute = ValueWithPostCompute(
            key = 1,
            calculate = { "value" to Unit },
            postCompute = { _, _, _ ->
                throw ProcessCanceledException()
            },
        )

        val pceOnFirstAccess = kotlin.runCatching { valueWithPostCompute.getValue() }.exceptionOrNull()
        Assertions.assertInstanceOf(ProcessCanceledException::class.java, pceOnFirstAccess)

        val pceOnSecondAccess = kotlin.runCatching { valueWithPostCompute.getValue() }.exceptionOrNull()
        Assertions.assertInstanceOf(ProcessCanceledException::class.java, pceOnSecondAccess)

        Assertions.assertNotEquals(pceOnFirstAccess, pceOnSecondAccess, "different PCE should be thrown on every access")
    }

    /**
     * 线程 t1 持有锁计算并进入 postCompute 时抛 PCE，等待中的 t2 必须重新计算而不是卡死或拿到半成品。
     */
    @Test
    fun testPCEFromPostCompute() {
        for (i in 1..100) {
            val t1CalledCalculate = CountDownLatch(1)
            val t2AccessedTheCache = CountDownLatch(1)

            val resultRef = AtomicReference<Any?>(null)

            val valueWithPostCompute = ValueWithPostCompute(
                key = 1,
                calculate = {
                    if (Thread.currentThread().name == "t1") {
                        t1CalledCalculate.countDown()
                    }
                    Thread.currentThread().name to Unit
                },
                postCompute = { _, _, _ ->
                    t2AccessedTheCache.await()
                    if (Thread.currentThread().name == "t1") {
                        throw ProcessCanceledException()
                    }
                },
            )

            val t1 = thread(name = "t1") {
                try {
                    valueWithPostCompute.getValue()
                } catch (_: ProcessCanceledException) {
                }
            }

            val t2 = thread(name = "t2") {
                t1CalledCalculate.await()
                t2AccessedTheCache.countDown()

                try {
                    resultRef.set(valueWithPostCompute.getValue())
                } catch (e: Throwable) {
                    resultRef.set(e)
                }
            }
            t2.join()
            t1.join()
            when (val result = resultRef.get()) {
                is Throwable -> throw result
                else -> Assertions.assertEquals("t2", result)
            }
        }
    }
}
