package org.cangnova.cangjie.cfir.session

import com.intellij.openapi.progress.ProcessCanceledException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class CfirCjmpSingleFlightCacheTest {
    @Test
    fun cancelledOwnerCannotPublishItsComputedValue() {
        val checks = AtomicInteger()
        val computationStarted = AtomicBoolean()
        val cache = CfirCjmpSingleFlightCache<String, String> {
            if (checks.incrementAndGet() == 2) throw ProcessCanceledException()
        }

        assertThrows(ProcessCanceledException::class.java) {
            cache.getOrCompute("sample") {
                computationStarted.set(true)
                "cancelled result"
            }
        }
        assertTrue(computationStarted.get())

        assertEquals("settled result", cache.getOrCompute("sample") { "settled result" })
        assertSame("settled result", cache.getOrCompute("sample") { error("cancelled result was published") })
    }

    @Test
    fun alreadyCancelledOwnerDoesNotStartTheComputation() {
        val computationStarted = AtomicBoolean()
        val cache = CfirCjmpSingleFlightCache<String, String> {
            throw ProcessCanceledException()
        }

        assertThrows(ProcessCanceledException::class.java) {
            cache.getOrCompute("sample") {
                computationStarted.set(true)
                "cancelled result"
            }
        }

        assertFalse(computationStarted.get())
    }

    @Test
    fun cancelledWaiterDoesNotCancelTheOwnerOrPoisonItsResult() {
        val executor = Executors.newFixedThreadPool(2)
        val computationStarted = CountDownLatch(1)
        val releaseComputation = CountDownLatch(1)
        val waiterThread = AtomicReference<Thread>()
        val waiterCancelled = AtomicBoolean()
        val waiterEnteredWait = CountDownLatch(1)
        val cache = CfirCjmpSingleFlightCache<String, String> {
            if (Thread.currentThread() === waiterThread.get()) {
                waiterEnteredWait.countDown()
                if (waiterCancelled.get()) throw ProcessCanceledException()
            }
        }

        try {
            val owner = executor.submit<String> {
                cache.getOrCompute("sample") {
                    computationStarted.countDown()
                    check(releaseComputation.await(30, TimeUnit.SECONDS))
                    "settled result"
                }
            }
            assertTrue(computationStarted.await(30, TimeUnit.SECONDS))

            val waiter = executor.submit<Throwable?> {
                waiterThread.set(Thread.currentThread())
                try {
                    cache.getOrCompute("sample") { "partial result" }
                    null
                } catch (failure: Throwable) {
                    failure
                }
            }
            assertTrue(waiterEnteredWait.await(30, TimeUnit.SECONDS))
            assertFalse(waiter.isDone)
            waiterCancelled.set(true)
            assertTrue(waiter.get(30, TimeUnit.SECONDS) is ProcessCanceledException)

            releaseComputation.countDown()
            assertEquals("settled result", owner.get(30, TimeUnit.SECONDS))
            assertSame("settled result", cache.getOrCompute("sample") { error("completed result was recomputed") })
        } finally {
            releaseComputation.countDown()
            executor.shutdownNow()
            assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS))
        }
    }

    @Test
    fun reentrantComputationCannotReadAnUnpublishedValueAndCanBeRetried() {
        val cache = CfirCjmpSingleFlightCache<String, String>()

        assertThrows(IllegalStateException::class.java) {
            cache.getOrCompute("sample") {
                cache.getOrCompute("sample") { "partial result" }
            }
        }

        assertEquals("settled result", cache.getOrCompute("sample") { "settled result" })
    }

}
