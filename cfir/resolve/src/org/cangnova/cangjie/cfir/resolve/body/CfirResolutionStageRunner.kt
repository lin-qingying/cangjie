package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidateApplicability
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirCheckerSinkImpl
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirResolutionContext

/**
 * 候选验证管线执行器。
 *
 * 对调用解析中的每个候选（CfirCandidate）按其 callKind 的 resolutionSequence
 * 顺序执行验证阶段，通过 CfirCheckerSinkImpl 收集诊断，
 * 支持 stopOnFirstError 提前退出。
 *
 * 对齐 K2 ResolutionStageRunner。
 */
class CfirResolutionStageRunner {

    /**
     * 对候选执行验证管线。
     *
     * 遍历 candidate.callInfo.callKind.resolutionSequence 中的每个阶段，
     * 各阶段通过 sink 报告诊断并可能触发提前退出。
     *
     * @return 候选通过管线后的最终适用性等级
     */
    fun processCandidate(
        candidate: CfirCandidate,
        context: CfirResolutionContext,
        stopOnFirstError: Boolean = true,
    ): CfirCandidateApplicability {
        val sink = CfirCheckerSinkImpl(candidate, stopOnFirstError)
        val stages = candidate.callInfo.callKind.resolutionSequence

        for (stage in stages) {
            if (sink.shouldStop) break
            stage.check(candidate, sink, context)
        }

        return candidate.lowestApplicability
    }
}
