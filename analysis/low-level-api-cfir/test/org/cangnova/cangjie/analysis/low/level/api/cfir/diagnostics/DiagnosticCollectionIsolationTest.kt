package org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * 诊断收集的异常隔离契约。
 *
 * [forEachWithDiagnosticIsolation] 是 `FileStructure.collectDiagnosticsFromStructureElements`
 * 实际使用的 element 级收集入口；该边界必须把单个元素的失败记录下来并继续，而不是让平台
 * logger 把异常升级为 AssertionError、终止整份文件的收集。
 */
class DiagnosticCollectionIsolationTest {
    /** 单个 checker/element 抛出的异常不得逃出隔离边界，且必须可被观测。 */
    @Test
    fun failingElementDoesNotEscape() {
        DiagnosticCollectionFailures.clearAll()
        assertNull(
            LLCfirDiagnosticVisitor.suppressAndLogExceptions<Unit> {
                throw IllegalStateException("simulated checker failure")
            },
        )
        assertEquals(1, DiagnosticCollectionFailures.drain().size)
    }

    /** 一个元素失败后，后续元素仍必须继续被收集，隔离异常可被逐元素观测。 */
    @Test
    fun laterElementsContinueAfterFailure() {
        DiagnosticCollectionFailures.clearAll()
        val collected = mutableListOf<String>()
        forEachWithDiagnosticIsolation(listOf("broken", "healthy")) { name ->
            if (name == "broken") {
                throw IllegalStateException("simulated checker failure")
            }
            collected += name
        }

        assertEquals(listOf("healthy"), collected)
        val failures = DiagnosticCollectionFailures.drain()
        assertEquals(1, failures.size)
        assertTrue(failures.single().message.orEmpty().contains("simulated checker failure"))
    }

    /** LSP worker 上记录的异常必须能由另一个测试/报告线程读取。 */
    @Test
    fun workerFailuresCanBeDrainedFromAnotherThread() {
        DiagnosticCollectionFailures.clearAll()
        val failure = IllegalStateException("worker-side checker failure")
        val callerFailure = IllegalStateException("caller-side checker failure")
        val worker = Executors.newSingleThreadExecutor()
        try {
            DiagnosticCollectionFailures.record(callerFailure)
            worker.submit { DiagnosticCollectionFailures.record(failure) }.get(5, TimeUnit.SECONDS)
            DiagnosticCollectionFailures.clear()
            assertSame(failure, DiagnosticCollectionFailures.drain().single())
        } finally {
            worker.shutdownNow()
            DiagnosticCollectionFailures.clearAll()
        }
    }

    /** 当前 worker 在 collection 中调用 drain 时只消费当前队列，不等待自身 collection 结束。 */
    @Test
    fun drainInsideActiveCollectionReturnsCurrentFailures() {
        DiagnosticCollectionFailures.clearAll()
        val failure = IllegalStateException("failure before in-collection drain")
        val worker = Executors.newSingleThreadExecutor()
        try {
            val drained = worker.submit<List<Throwable>> {
                DiagnosticCollectionFailures.withCollection {
                    DiagnosticCollectionFailures.record(failure)
                    DiagnosticCollectionFailures.drain()
                }
            }.get(5, TimeUnit.SECONDS)

            assertSame(failure, drained.single())
            assertTrue(DiagnosticCollectionFailures.drain().isEmpty())
        } finally {
            worker.shutdownNow()
            worker.awaitTermination(5, TimeUnit.SECONDS)
            DiagnosticCollectionFailures.clearAll()
        }
    }

    /** collection 外的 drain 等待其它 worker 批次完成后再汇总失败。 */
    @Test
    fun drainWaitsForOtherActiveCollections() {
        DiagnosticCollectionFailures.clearAll()
        val failure = IllegalStateException("failure in active worker collection")
        val collectionStarted = CountDownLatch(1)
        val finishCollection = CountDownLatch(1)
        val worker = Executors.newSingleThreadExecutor()
        val drainer = Executors.newSingleThreadExecutor()
        try {
            val workerTask = worker.submit {
                DiagnosticCollectionFailures.withCollection {
                    DiagnosticCollectionFailures.record(failure)
                    collectionStarted.countDown()
                    check(finishCollection.await(5, TimeUnit.SECONDS))
                }
            }
            assertTrue(collectionStarted.await(5, TimeUnit.SECONDS))

            val drainStarted = CountDownLatch(1)
            val drainTask = drainer.submit<List<Throwable>> {
                drainStarted.countDown()
                DiagnosticCollectionFailures.drain()
            }
            assertTrue(drainStarted.await(5, TimeUnit.SECONDS))
            org.junit.jupiter.api.Assertions.assertThrows(TimeoutException::class.java) {
                drainTask.get(200, TimeUnit.MILLISECONDS)
            }

            finishCollection.countDown()
            workerTask.get(5, TimeUnit.SECONDS)
            assertSame(failure, drainTask.get(5, TimeUnit.SECONDS).single())
        } finally {
            finishCollection.countDown()
            worker.shutdownNow()
            drainer.shutdownNow()
            worker.awaitTermination(5, TimeUnit.SECONDS)
            drainer.awaitTermination(5, TimeUnit.SECONDS)
            DiagnosticCollectionFailures.clearAll()
        }
    }

    /** workspace 批次内逐文件调用 clear 不得抹掉较早文件已记录的异常。 */
    @Test
    fun fileClearsDoNotDiscardFailuresInsideOneCollection() {
        DiagnosticCollectionFailures.clearAll()
        DiagnosticCollectionFailures.withCollection {
            DiagnosticCollectionFailures.record(IllegalStateException("first file"))
            DiagnosticCollectionFailures.clear()
            DiagnosticCollectionFailures.record(IllegalStateException("second file"))
        }
        val failures = DiagnosticCollectionFailures.drain()

        assertEquals(listOf("first file", "second file"), failures.map { it.message })
        DiagnosticCollectionFailures.clearAll()
    }

    /** 生产批次依赖隔离边界的即时日志，不把异常对象长期留在进程级测试记录器中。 */
    @Test
    fun productionCollectionsDoNotRetainFailures() {
        DiagnosticCollectionFailures.clearAll()
        DiagnosticCollectionFailures.withCollection(retainFailures = false) {
            DiagnosticCollectionFailures.withCollection {
                DiagnosticCollectionFailures.record(IllegalStateException("production checker failure"))
            }
        }

        assertTrue(DiagnosticCollectionFailures.drain().isEmpty())
        DiagnosticCollectionFailures.clearAll()
    }
}
