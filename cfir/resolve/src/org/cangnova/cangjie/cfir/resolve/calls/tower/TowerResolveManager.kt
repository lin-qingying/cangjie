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

class TowerResolveManager private constructor(
    private val shouldStopAtTheLevel: (CfirTowerGroup) -> Boolean,
) {
    constructor(collector: CfirCandidateCollector) : this(collector::shouldStopAtTheGroup)

    private val queue = PriorityQueue<SuspendedResolverTask>()

    fun reset() {
        queue.clear()
    }

    private suspend fun suspendResolverTask(group: CfirTowerGroup) =
        suspendCoroutineUninterceptedOrReturn<Unit> {
            queue += SuspendedResolverTask(it, group)
            COROUTINE_SUSPENDED
        }

    suspend fun requestGroup(requested: CfirTowerGroup) {
        if (shouldStopAtTheLevel(requested)) {
            stopResolverTask()
        }
        val peeked = queue.peek()
        if (peeked != null && peeked.group <= requested) {
            suspendResolverTask(requested)
        }
    }

    private suspend fun stopResolverTask(): Nothing =
        suspendCoroutineUninterceptedOrReturn { COROUTINE_SUSPENDED }

    fun enqueueResolverTask(
        group: CfirTowerGroup = CfirTowerGroup.Start,
        task: suspend () -> Unit,
    ) {
        val continuation = task.createCoroutineUnintercepted(
            object : Continuation<Unit> {
                override val context: CoroutineContext
                    get() = EmptyCoroutineContext

                override fun resumeWith(result: Result<Unit>) {
                    result.getOrThrow()
                }
            },
        )

        queue += SuspendedResolverTask(continuation, group)
    }

    private fun resumeTask(task: SuspendedResolverTask) {
        if (shouldStopAtTheLevel(task.group)) return
        task.continuation.resume(Unit)
    }

    fun runTasks() {
        while (queue.isNotEmpty()) {
            resumeTask(queue.poll())
        }
    }

    private data class SuspendedResolverTask(
        val continuation: Continuation<Unit>,
        val group: CfirTowerGroup,
    ) : Comparable<SuspendedResolverTask> {
        override fun compareTo(other: SuspendedResolverTask): Int = group.compareTo(other.group)
    }
}
