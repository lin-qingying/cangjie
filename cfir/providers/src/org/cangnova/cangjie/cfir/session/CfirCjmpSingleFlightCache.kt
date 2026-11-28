package org.cangnova.cangjie.cfir.session

import com.intellij.openapi.progress.ProgressManager
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * 每个键只由一个线程准备并发布完整的 CJMP 派生结果；其它线程等待同一个不可变结果。
 * 失败的计算不进入缓存，递归读取尚未发布的同键结果会立即失败，避免死锁或观察半成品。
 */
internal class CfirCjmpSingleFlightCache<K : Any, V : Any>(
    private val checkCanceled: () -> Unit = { ProgressManager.checkCanceled() },
) {
    private val computations = ConcurrentHashMap<K, Computation<V>>()

    fun getOrCompute(key: K, compute: () -> V): V {
        computations[key]?.let { return it.await() }

        val candidate = Computation<V>(Thread.currentThread(), checkCanceled)
        val computation = computations.putIfAbsent(key, candidate) ?: candidate
        if (computation !== candidate) return computation.await()

        try {
            checkCanceled()
            val value = compute()
            checkCanceled()
            candidate.result.complete(value)
            return value
        } catch (failure: Throwable) {
            candidate.result.completeExceptionally(failure)
            computations.remove(key, candidate)
            throw failure
        }
    }

    private class Computation<V : Any>(
        private val ownerThread: Thread,
        private val checkCanceled: () -> Unit,
    ) {
        val result = CompletableFuture<V>()

        fun await(): V {
            check(result.isDone || ownerThread !== Thread.currentThread()) {
                "CJMP computation cannot recursively observe an unpublished result"
            }
            while (true) {
                checkCanceled()
                try {
                    return result.get(WAITING_INTERVAL_MILLIS, TimeUnit.MILLISECONDS)
                } catch (_: TimeoutException) {
                    // 等待过程中定期检查 IDE 的取消状态。
                } catch (interrupted: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw CancellationException("Interrupted while waiting for a CJMP computation").also {
                        it.initCause(interrupted)
                    }
                } catch (failure: ExecutionException) {
                    throw failure.cause ?: failure
                }
            }
        }
    }

    private companion object {
        const val WAITING_INTERVAL_MILLIS = 100L
    }
}
