package org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * 锁定 builtins session 创建守卫的三个契约：
 * 同线程重入拿到同一个半成品、登记在异常路径上也会清除、不同线程互不影响。
 */
class BuiltinsSessionCreationGuardTest {
    /**
     * 同线程重入时拿到的是同一个半成品 session，而不是再走一次创建。
     */
    @Test
    fun reentrantAccessReturnsTheSameHalfBakedSession() {
        val guard = BuiltinsSessionCreationGuard<String, String>()

        val outer = guard.withSessionUnderConstruction("stdlib", "session-A") {
            assertEquals("session-A", guard.halfBakedSessionFor("stdlib"))
            assertTrue(guard.hasSessionUnderConstruction())
            guard.halfBakedSessionFor("stdlib")
        }

        assertEquals("session-A", outer)
        assertNull(guard.halfBakedSessionFor("stdlib"), "registration must be cleared once creation finished")
    }

    /**
     * 正常退出与异常退出都要清除登记，否则半成品 session 会泄漏到后续调用。
     */
    @Test
    fun registrationIsClearedOnNormalAndExceptionalExit() {
        val guard = BuiltinsSessionCreationGuard<String, String>()

        guard.withSessionUnderConstruction("stdlib", "session-A") { }
        assertNull(guard.halfBakedSessionFor("stdlib"))
        assertFalse(guard.hasSessionUnderConstruction())

        runCatching { guard.withSessionUnderConstruction("stdlib", "session-B") { error("creation failed") } }
        assertNull(guard.halfBakedSessionFor("stdlib"))
        assertFalse(guard.hasSessionUnderConstruction())
    }

    /**
     * 不同键可以同时处于创建中（例如两个目标平台的 builtins session），互不干扰。
     */
    @Test
    fun differentKeysAreTrackedIndependently() {
        val guard = BuiltinsSessionCreationGuard<String, String>()

        guard.withSessionUnderConstruction("linux", "session-Linux") {
            guard.withSessionUnderConstruction("windows", "session-Windows") {
                assertEquals("session-Linux", guard.halfBakedSessionFor("linux"))
                assertEquals("session-Windows", guard.halfBakedSessionFor("windows"))
            }
            assertNull(guard.halfBakedSessionFor("windows"))
            assertEquals("session-Linux", guard.halfBakedSessionFor("linux"))
        }
    }

    /**
     * 守卫只跟踪当前线程：另一个线程的创建状态对本线程不可见。
     */
    @Test
    fun guardIsThreadLocal() {
        val guard = BuiltinsSessionCreationGuard<String, String>()
        val created = CountDownLatch(1)
        val release = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()

        try {
            val future = executor.submit<String> {
                guard.withSessionUnderConstruction("stdlib", "session-Other") {
                    created.countDown()
                    release.await(10, TimeUnit.SECONDS)
                }
                "session-Other"
            }
            assertTrue(created.await(10, TimeUnit.SECONDS))
            assertNull(guard.halfBakedSessionFor("stdlib"), "another thread's creation must not be visible")
            release.countDown()
            assertEquals("session-Other", future.get(10, TimeUnit.SECONDS))
            assertNull(guard.halfBakedSessionFor("stdlib"))
        } finally {
            release.countDown()
            executor.shutdownNow()
        }
    }

    /**
     * 同一键在登记状态下再次登记属于编程错误：调用方本应先走重入短路。
     */
    @Test
    fun doubleRegistrationOfTheSameKeyFails() {
        val guard = BuiltinsSessionCreationGuard<String, String>()

        val error = runCatching {
            guard.withSessionUnderConstruction("stdlib", "session-A") {
                guard.withSessionUnderConstruction("stdlib", "session-B") { }
            }
        }.exceptionOrNull()

        assertTrue(error is IllegalStateException, "expected IllegalStateException, got $error")
        assertNull(guard.halfBakedSessionFor("stdlib"))
    }
}