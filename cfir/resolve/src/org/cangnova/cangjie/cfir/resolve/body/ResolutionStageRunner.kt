package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.stages.CheckerSinkImpl
import org.cangnova.cangjie.cfir.session.inferenceLogger
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.intrinsics.createCoroutineUnintercepted
import kotlin.coroutines.resume
import kotlin.inc
import kotlin.text.compareTo
import kotlin.text.get

/**
 * 候选验证管线执行器。
 * 它会按 `callKind.resolutionSequence` 的顺序执行所有验证阶段，
 * 通过 `CheckerSinkImpl` 收集诊断，并支持 `stopOnFirstError` 提前退出。
 * 对齐 K2 `ResolutionStageRunner`。
 */
class ResolutionStageRunner {

    /**
     * 对候选执行验证管线。
     * 它会依次遍历 `candidate.callInfo.callKind.resolutionSequence` 中的阶段，
     * 并通过 sink 上报诊断。
     * @return 候选通过管线后的最终适用性等级
     */
    fun processCandidate(
        candidate: Candidate,
        context: ResolutionContext,
        stopOnFirstError: Boolean = true,
        runAdditionalStages: Boolean = false,

    ): CandidateApplicability {
        val sink = CheckerSinkImpl(candidate, stopOnFirstError = stopOnFirstError)
        val inferenceLogger = candidate.callInfo.session.inferenceLogger
        inferenceLogger?.logCandidate(candidate)
        var finished = false

        sink.continuation = suspend {
            // Multiple runs on the same candidate are possible,
            // that's why we have to skip already processed stages on the next run.
            // Neither regular `for` loop nor iterating by index don't work here,
            // because we have to start from the next unprocessed stage and mutate `Candidate.passedStages` on every iteration.
            val resolutionSequence = candidate.callInfo.callKind.let {
                if (runAdditionalStages) it.resolutionSequenceWithAdditionalStages
                else it.resolutionSequence
            }
            while (candidate.passedStages < resolutionSequence.size) {
                context(context, sink) {
                    val nextStage = resolutionSequence[candidate.passedStages++]
                    inferenceLogger?.logStage("Resolution Stages > ${nextStage::class.simpleName}", candidate.system)
                    nextStage.check(candidate)
                }
            }
        }.createCoroutineUnintercepted(completion = object : Continuation<Unit> {
            override val context: CoroutineContext
                get() = EmptyCoroutineContext

            override fun resumeWith(result: Result<Unit>) {
                result.exceptionOrNull()?.let { throw it }
                finished = true
            }
        })

        while (!finished) {
            sink.continuation!!.resume(Unit)
            if (!candidate.isSuccessful) {
                break
            }
        }
        return candidate.lowestApplicability
    }
}

/**
 * 完整处理候选，收集所有诊断而不提前停止。
 * 对齐 K2 `fullyProcessCandidate` 扩展函数。
 */
fun ResolutionStageRunner.fullyProcessCandidate(candidate: Candidate, resolutionContext: ResolutionContext) {
    if (candidate.passedStages < candidate.callInfo.callKind.resolutionSequence.size) {
        processCandidate(candidate, resolutionContext, stopOnFirstError = false)
    }
}
