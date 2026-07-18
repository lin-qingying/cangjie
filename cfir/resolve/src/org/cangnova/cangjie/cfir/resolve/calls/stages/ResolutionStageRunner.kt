/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.resolve.calls.CandidateProcessingMode
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.session.inferenceLogger
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.intrinsics.createCoroutineUnintercepted
import kotlin.coroutines.resume

/** 顺序执行候选 resolution stage 的 runner。 */
class ResolutionStageRunner {
    /**
     * 处理单个候选并返回其最低适用性。
     *
     * 该方法支持遇到首个错误即停止，也支持额外阶段运行；stage coroutine 会在 checker sink 要求 yield 时暂停。
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
            val stageLimit = when (context.candidateProcessingMode) {
                CandidateProcessingMode.FULL -> resolutionSequence.size
                CandidateProcessingMode.ARGUMENT_SHAPE -> {
                    val mapArgumentsIndex = resolutionSequence.indexOf(CfirMapArguments)
                    if (mapArgumentsIndex >= 0) mapArgumentsIndex + 1 else resolutionSequence.size
                }
            }
            while (candidate.passedStages < stageLimit) {
                context(context, sink) {
                    val nextStage = resolutionSequence[candidate.passedStages++]
                    inferenceLogger?.logStage("Resolution Stages > ${nextStage::class.simpleName}", candidate.system)
                    nextStage.check(candidate)
                }
            }
        }.createCoroutineUnintercepted(completion = object : Continuation<Unit> {
            override val context: CoroutineContext
                get() = EmptyCoroutineContext

            /** 完成 stage coroutine，并把异常重新抛给同步调用方。 */
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
