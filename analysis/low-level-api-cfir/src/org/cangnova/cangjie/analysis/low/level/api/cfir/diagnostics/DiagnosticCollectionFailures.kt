package org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * low-level 诊断收集中被隔离的异常记录。
 *
 * 单个 checker / structure element 失败时，收集流程必须继续处理其它元素；但“继续”不能
 * 等价于“没有失败”。这里把被隔离的异常显式记录下来，测试和真实项目报告可以断言
 * 收集期间没有发生任何被吞掉的异常，避免异常门禁失效。
 *
 * 收集异常按 worker 隔离记录；clear 只重置调用线程，drain 汇总所有已完成的 worker 收集。
 * 生产 LSP 请求由隔离边界即时写日志，因此可关闭长期保留；测试请求保留记录供主线程 drain。
 */
object DiagnosticCollectionFailures {
    /**
     * 一个 worker 的异常清理代次。
     *
     * 全局队列只持有该轻量 token，不持有 Thread 或其 ThreadLocalMap；worker 退出后，
     * 未被 drain 的失败仍可观测，但不会因此把 Thread 对象留在进程级单例中。
     */
    private class ThreadFailureState(var generation: Long = 0L)

    /**
     * 异常记录及其所属 worker 的清理代次。
     */
    private data class RecordedFailure(
        val owner: ThreadFailureState,
        val generation: Long,
        val failure: Throwable,
    )

    /**
     * 异常队列和正在进行的收集批次数，由同一把锁保护。
     */
    private val lock = ReentrantLock()
    private val allCollectionsFinished = lock.newCondition()
    private val failures = mutableListOf<RecordedFailure>()
    private val threadFailureState = ThreadLocal.withInitial { ThreadFailureState() }
    private val collectionDepth = ThreadLocal.withInitial { 0 }
    private val collectionRetainsFailures = ThreadLocal.withInitial { true }
    private var activeCollections: Int = 0

    /**
     * 记录一个被隔离的异常。
     */
    fun record(failure: Throwable) {
        if (collectionDepth.get() > 0 && !collectionRetainsFailures.get()) return
        val owner = threadFailureState.get()
        lock.withLock {
            failures += RecordedFailure(owner, owner.generation, failure)
        }
    }

    /**
     * 取出并清空所有线程尚未被消费的隔离异常。
     *
     * collection 外等待所有活动批次结束后返回完整快照；当前线程处于批次内时立即返回当前快照，
     * 避免等待自己的批次结束。此时其它 worker 后续记录的异常由下一次调用读取。
     */
    fun drain(): List<Throwable> {
        return lock.withLock {
            // 同一批次内 drain 等待的正是自己的 withCollection 返回，属于条件自等待。
            // 批次内返回当前已记录项；其它 worker 后续记录的异常留待下一次 drain。
            if (collectionDepth.get() == 0) {
                while (activeCollections > 0) {
                    allCollectionsFinished.await()
                }
            }
            drainRecordedFailures()
        }
    }

    /**
     * 清空调用线程当前代次的异常，供一次收集开始前重置状态；collection 内调用保持 no-op，避免丢弃批次记录。
     */
    fun clear() {
        if (collectionDepth.get() > 0) return
        val owner = threadFailureState.get()
        lock.withLock {
            owner.generation++
            failures.removeAll { failure -> failure.owner === owner }
        }
    }

    /** 清空所有 worker 的记录；测试隔离使用，不能与诊断收集并发调用。 */
    fun clearAll() {
        lock.withLock {
            check(activeCollections == 0) {
                "Cannot clear diagnostic failures while collections are active"
            }
            failures.clear()
        }
    }

    /** 在持锁状态下消费队列，并忽略已由所属 worker 清理的代次。 */
    private fun drainRecordedFailures(): List<Throwable> = buildList {
        val iterator = failures.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            iterator.remove()
            if (entry.generation == entry.owner.generation) {
                add(entry.failure)
            }
        }
    }

    /**
     * 将可能跨文件的诊断收集作为一个异常记录批次；批次内的文件级 clear 不会丢弃前序文件异常。
     * 生产请求可关闭记录保留：隔离边界仍写日志，但不把 Throwable 留在进程级记录器中。
     */
    fun <T> withCollection(retainFailures: Boolean = true, block: () -> T): T {
        val previousDepth = collectionDepth.get()
        val previousRetention = collectionRetainsFailures.get()
        if (previousDepth == 0) {
            lock.withLock { activeCollections++ }
        }
        collectionDepth.set(previousDepth + 1)
        collectionRetainsFailures.set(previousRetention && retainFailures)
        return try {
            block()
        } finally {
            if (previousDepth == 0) {
                collectionDepth.remove()
                collectionRetainsFailures.remove()
                lock.withLock {
                    activeCollections--
                    if (activeCollections == 0) allCollectionsFinished.signalAll()
                }
            } else {
                collectionDepth.set(previousDepth)
                collectionRetainsFailures.set(previousRetention)
            }
        }
    }
}
