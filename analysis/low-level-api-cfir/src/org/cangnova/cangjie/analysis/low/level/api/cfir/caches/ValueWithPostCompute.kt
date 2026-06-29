

package org.cangnova.cangjie.analysis.low.level.api.cfir.caches

import org.cangnova.cangjie.analysis.low.level.api.cfir.util.lockWithPCECheck
import java.util.concurrent.locks.ReentrantLock

/**
 * Lazily calculated value which runs postCompute in the same thread,
 * assuming that postCompute may try to read that value inside current thread,
 * So in the period then value is calculated but post compute was not finished,
 * only thread that initiated the calculating may see the value,
 * other threads will have to wait until that value is calculated
 */
internal class ValueWithPostCompute<KEY, VALUE, DATA>(
    /**
     * We need at least one final field to be written in constructor to guarantee safe initialization of our [ValueWithPostCompute]
     */
    private val key: KEY,
    calculate: (KEY) -> Pair<VALUE, DATA>,
    postCompute: (KEY, VALUE, DATA) -> Unit,
) {
    /**
     * 尚未完成计算时持有的 value 计算函数，计算完成后会被清空以释放闭包。
     */
    private var _calculate: ((KEY) -> Pair<VALUE, DATA>)? = calculate

    /**
     * 尚未完成 post-compute 时持有的后处理函数，完成后会被清空。
     */
    private var _postCompute: ((KEY, VALUE, DATA) -> Unit)? = postCompute

    /**
     * [lock] being volatile ensures the consistent reads between [lock] and [value] in different threads.
     */
    @Volatile
    private var lock: ReentrantLock? = ReentrantLock()

    /**
     * can be in one of the following three states:
     * [ValueIsNotComputed] -- value is not initialized and thread are now executing [_postCompute]
     * [ValueIsPostComputingNow] -- thread with threadId has computed the value and only it can access it during post compute
     * some value of type [VALUE] -- value is computed and post compute was executed, values is visible for all threads
     *
     * Value may be set only under [ValueWithPostCompute] intrinsic lock hold
     * And may be read from any thread
     */
    @Volatile
    private var value: Any? = ValueIsNotComputed

    /**
     * 防止 value 计算函数在同一锁持有期间递归读取自身。
     */
    private inline fun <T> recursiveGuarded(body: () -> T): T {
        check(lock!!.holdCount == 1) {
            "Should not be called recursively"
        }
        return body()
    }

    /**
     * 返回 value；如尚未计算则由当前线程完成计算并执行 post-compute。
     */
    @Suppress("UNCHECKED_CAST")
    fun getValue(): VALUE = when (val stateSnapshot = value) {
        is ValueIsPostComputingNow -> {
            if (stateSnapshot.threadId == Thread.currentThread().id) {
                stateSnapshot.value as VALUE
            } else {
                // wait until another thread that holds the lock now computes the value
                lock?.lockWithPCECheck {
                    when (value) {
                        ValueIsNotComputed -> {
                            // if we have a PCE during value computation,
                            // then we will enter the critical section with `value == ValueIsNotComputed`
                            // in this case, we should try to recalculate the value
                            computeValueWithoutLock()
                        }

                        // another thread computed the value for us
                        else -> value as VALUE
                    }
                } ?: (value as VALUE)
            }
        }
        ValueIsNotComputed -> lock?.lockWithPCECheck {
            computeValueWithoutLock()
        } ?: (value as VALUE)

        else -> stateSnapshot as VALUE
    }

    @Suppress("UNCHECKED_CAST")
    // should be called under a synchronized section
    /**
     * 在持有锁的情况下完成 value 计算、post-compute 执行和最终状态发布。
     */
    private fun computeValueWithoutLock(): VALUE {
        // if we entered synchronized section that's mean that the value is not yet calculated and was not started to be calculated
        // or the some other thread calculated the value while we were waiting to acquire the lock

        when (value) {
            ValueIsNotComputed -> {
                // will be computed later, the read of `ValueIsNotComputed` guarantees that lock is not null
                require(lock!!.isHeldByCurrentThread)
            }
            else -> {
                // other thread computed the value for us and set `lock` to null
                require(lock == null)
                return value as VALUE
            }
        }

        val calculatedValue = try {
            val (calculated, data) = recursiveGuarded {
                _calculate!!(key)
            }
            value = ValueIsPostComputingNow(calculated, Thread.currentThread().id) // only current thread may see the value
            _postCompute!!(key, calculated, data)
            calculated
        } catch (e: Throwable) {
            value = ValueIsNotComputed
            throw e
        }
        // reading lock = null implies that the value is calculated and stored
        value = calculatedValue
        _calculate = null
        _postCompute = null
        lock = null

        return calculatedValue
    }

    /**
     * 仅当 value 已完整计算并完成 post-compute 时返回结果。
     */
    @Suppress("UNCHECKED_CAST")
    fun getValueIfComputed(): VALUE? = when (value) {
        ValueIsNotComputed -> null
        is ValueIsPostComputingNow -> null
        else -> value as VALUE
    }

    /**
     * 表示当前线程已经算出 value、但 post-compute 尚未完成的中间状态。
     */
    private class ValueIsPostComputingNow(val value: Any?, val threadId: Long)

    /**
     * 表示 value 尚未开始或尚未成功完成计算的初始状态。
     */
    private object ValueIsNotComputed
}
