package org.cangnova.cangjie

import com.intellij.mock.MockApplication
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.ThreadingSupport
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.ThrowableComputable
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** 使用真实 Core application 与 SDK 平台锁核验两个模式，不替换宏引擎或语义实现。 */
class CangJieCoreReadWriteActionContractTest {
    @Test
    fun `writer waits for full reader and excludes new readers in both modes`() = inBothModes { application ->
        val executor = Executors.newFixedThreadPool(2)
        val readerEntered = CountDownLatch(1)
        val releaseReader = CountDownLatch(1)
        val writerEntered = CountDownLatch(1)
        val releaseWriter = CountDownLatch(1)
        val forbiddenReadRan = AtomicBoolean()
        try {
            val reader = executor.submit(Callable {
                application.runReadAction(Computable {
                    assertTrue(application.isReadAccessAllowed)
                    assertTrue(application.holdsReadLock())
                    assertTrue(requireNotNull(application.getService(ThreadingSupport::class.java)).isReadLockedByThisThread())
                    assertFalse(application.isWriteAccessAllowed)
                    readerEntered.countDown()
                    assertTrue(releaseReader.await(10, TimeUnit.SECONDS), "reader coordination must be released")
                    41
                })
            })
            assertTrue(readerEntered.await(10, TimeUnit.SECONDS), "actual read action must start")
            val writer = executor.submit(Callable {
                application.runWriteAction(Computable {
                    assertTrue(application.isWriteAccessAllowed)
                    assertTrue(application.isReadAccessAllowed)
                    writerEntered.countDown()
                    assertTrue(releaseWriter.await(10, TimeUnit.SECONDS), "writer coordination must be released")
                    42
                })
            })
            awaitActualPendingWrite(application)
            assertEquals(1L, writerEntered.count, "a pending writer cannot pass an active read action")
            releaseReader.countDown()
            assertEquals(41, reader.get(10, TimeUnit.SECONDS))
            assertTrue(writerEntered.await(10, TimeUnit.SECONDS), "actual writer must acquire after reader exit")
            assertFalse(application.tryRunReadAction { forbiddenReadRan.set(true) })
            assertFalse(forbiddenReadRan.get(), "tryRead must not run while another thread owns write access")
            releaseWriter.countDown()
            assertEquals(42, writer.get(10, TimeUnit.SECONDS))
            assertFalse(application.isWriteAccessAllowed)
            assertFalse(application.holdsReadLock())
        } finally {
            releaseReader.countDown()
            releaseWriter.countDown()
            executor.shutdownNow()
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS), "reader/writer tasks must finish")
        }
    }

    @Test
    fun `nested write retains actual permission and permits nested read in both modes`() = inBothModes { application ->
        assertFalse(application.isWriteAccessAllowed)
        application.runWriteAction {
            assertTrue(application.isWriteAccessAllowed)
            application.runWriteAction {
                assertTrue(application.isWriteAccessAllowed)
            }
            assertTrue(application.isWriteAccessAllowed, "nested write exit must retain the outer write permit")
            application.runReadAction {
                assertTrue(application.isReadAccessAllowed)
                assertTrue(application.isWriteAccessAllowed)
            }
            assertTrue(application.isWriteAccessAllowed)
        }
        assertFalse(application.isWriteAccessAllowed)
    }

    @Test
    fun `read to write upgrade is rejected by platform in both modes`() = inBothModes { application ->
        application.runReadAction {
            assertTrue(application.holdsReadLock())
            val failure = assertThrows(IllegalStateException::class.java) {
                application.runWriteAction {}
            }
            assertEquals("WriteAction can not be called from ReadAction", failure.message)
            assertTrue(application.holdsReadLock(), "upgrade rejection must retain the original read permit")
        }
        application.runWriteAction {
            assertTrue(application.isWriteAccessAllowed, "the rejected upgrade must not strand the platform lock")
        }
    }

    @Test
    fun `PCE and checked exceptions preserve identity and release permits in both modes`() = inBothModes { application ->
        val cancellation = ProcessCanceledException()
        val thrownCancellation = assertThrows(ProcessCanceledException::class.java) {
            application.runReadAction(Computable<Any?> { throw cancellation })
        }
        assertSame(cancellation, thrownCancellation, "the original PCE must propagate unchanged")
        val checkedFailure = IOException("real checked write failure")
        val thrownCheckedFailure = assertThrows(IOException::class.java) {
            application.runWriteAction(ThrowableComputable<Any?, IOException?> { throw checkedFailure })
        }
        assertSame(checkedFailure, thrownCheckedFailure, "ThrowableComputable must not wrap the original exception")
        application.runWriteAction { assertTrue(application.isWriteAccessAllowed) }
        application.runReadAction { assertTrue(application.holdsReadLock()) }
    }

    @Test
    fun `nonblocking read holds actual permit and completes UI callback after release in both modes`() = inBothModes { application ->
        val callbackRan = AtomicBoolean()
        val value = ReadAction.nonBlocking(Callable {
            assertTrue(application.holdsReadLock(), "nonblocking calculation must use the same platform read permit")
            43
        }).finishOnUiThread(ModalityState.any()) {
            assertFalse(application.holdsReadLock(), "UI callback must run after the calculation releases read access")
            application.runWriteAction {
                assertTrue(application.isWriteAccessAllowed)
                callbackRan.set(true)
            }
        }.executeSynchronously()
        assertEquals(43, value)
        assertTrue(callbackRan.get())
    }

    /** 只接受平台报告的真实 pending；协调超时是失败，不代表写屏障已完成。 */
    private fun awaitActualPendingWrite(application: MockApplication) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (!application.isWriteActionPending) {
            check(System.nanoTime() < deadline) { "actual platform writer did not become pending" }
            Thread.yield()
        }
    }

    private fun inBothModes(action: (MockApplication) -> Unit) {
        for (mode in listOf(CangJieCoreEnvironmentMode.Production, CangJieCoreEnvironmentMode.UnitTest)) {
            val root = Disposer.newDisposable("actual Core read/write gate $mode")
            try {
                val environment = CangJieCoreEnvironment.create(root, mode)
                val application = environment.applicationEnvironment.application
                assertEquals(mode == CangJieCoreEnvironmentMode.UnitTest, application.isUnitTestMode)
                action(application)
            } finally {
                ApplicationManager.getApplication().runWriteAction { Disposer.dispose(root) }
            }
        }
    }
}
