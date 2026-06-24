package org.cangnova.cangjie.cfir.resolve.calls.tower

import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidateCollector
import java.util.PriorityQueue
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.createCoroutineUnintercepted
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.coroutines.resume

/**
 * tower resolve 的协程任务调度器。
 *
 * 解析器把不同 tower group 的查找任务挂起到优先队列中，
 * 当某一层已经得到足够候选时通过 [shouldStopAtTheLevel] 停止更低优先级层级继续执行。
 */
class TowerResolveManager private constructor(
    /**
     * 判断指定 tower group 是否应停止继续解析的谓词。
     */
    private val shouldStopAtTheLevel: (CfirTowerGroup) -> Boolean,
) {
    /**
     * 使用候选收集器的停止策略创建调度器。
     */
    constructor(collector: CfirCandidateCollector) : this(collector::shouldStopAtTheGroup)

    /**
     * 按 tower group 优先级排列的挂起解析任务队列。
     */
    private val queue = PriorityQueue<SuspendedResolverTask>()

    /**
     * 清空所有已挂起解析任务。
     */
    fun reset() {
        queue.clear()
    }

    /**
     * 将当前协程挂起为指定 tower group 的解析任务。
     */
    private suspend fun suspendResolverTask(group: CfirTowerGroup) =
        suspendCoroutineUninterceptedOrReturn<Unit> {
            queue += SuspendedResolverTask(it, group)
            COROUTINE_SUSPENDED
        }

    /**
     * 请求进入指定 tower group。
     *
     * 如果更高或同等优先级任务尚未完成，当前任务会挂起等待调度。
     */
    suspend fun requestGroup(requested: CfirTowerGroup) {
        if (shouldStopAtTheLevel(requested)) {
            stopResolverTask()
        }
        val peeked = queue.peek()
        if (peeked != null && peeked.group <= requested) {
            suspendResolverTask(requested)
        }
    }

    /**
     * 永久挂起当前解析任务，用于在已有足够候选时停止后续层级。
     */
    private suspend fun stopResolverTask(): Nothing =
        suspendCoroutineUninterceptedOrReturn { COROUTINE_SUSPENDED }

    /**
     * 将一个解析任务注册到指定 tower group。
     */
    fun enqueueResolverTask(
        group: CfirTowerGroup = CfirTowerGroup.Start,
        task: suspend () -> Unit,
    ) {
        val continuation = task.createCoroutineUnintercepted(
            /** 将 suspend tower 任务包装成可由本地优先队列恢复的 continuation。 */
            object : Continuation<Unit> {
                /** tower resolve 任务不依赖外部 coroutine context。 */
                override val context: CoroutineContext
                    get() = EmptyCoroutineContext

                /** 恢复任务后立即重新抛出失败，避免调度队列吞掉解析异常。 */
                override fun resumeWith(result: Result<Unit>) {
                    result.getOrThrow()
                }
            },
        )

        queue += SuspendedResolverTask(continuation, group)
    }

    /**
     * 恢复单个解析任务；若其 group 已满足停止条件则丢弃。
     */
    private fun resumeTask(task: SuspendedResolverTask) {
        if (shouldStopAtTheLevel(task.group)) return
        task.continuation.resume(Unit)
    }

    /**
     * 按优先级运行队列中的所有解析任务。
     */
    fun runTasks() {
        while (queue.isNotEmpty()) {
            resumeTask(queue.poll())
        }
    }

    /**
     * 一个已挂起的 tower resolve 任务。
     */
    private data class SuspendedResolverTask(
        /**
         * 恢复任务执行的 continuation。
         */
        val continuation: Continuation<Unit>,
        /**
         * 任务所属的 tower group。
         */
        val group: CfirTowerGroup,
    ) : Comparable<SuspendedResolverTask> {
        /**
         * 按 tower group 优先级排序。
         */
        override fun compareTo(other: SuspendedResolverTask): Int = group.compareTo(other.group)
    }
}
