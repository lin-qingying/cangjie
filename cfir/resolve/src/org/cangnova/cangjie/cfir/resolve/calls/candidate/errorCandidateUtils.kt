package org.cangnova.cangjie.cfir.resolve.calls.candidate

import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.stages.ResolutionStageRunner
import org.cangnova.cangjie.cfir.resolve.calls.stages.fullyProcessCandidate
import org.cangnova.cangjie.source.CjSourceElement

/** 基于诊断创建错误候选并包装成带候选的错误引用。 */
fun createErrorReferenceWithErrorCandidate(
    callInfo: CallInfo,
    diagnostic: ConeDiagnostic,
    source: CjSourceElement?,
    resolutionContext: ResolutionContext,
    resolutionStageRunner: ResolutionStageRunner,
): CfirErrorReferenceWithCandidate {
    return CfirErrorReferenceWithCandidate(
        source = source,
        name = callInfo.name,
        candidate = resolutionStageRunner.createErrorCandidate(callInfo, resolutionContext, diagnostic),
        diagnostic = diagnostic,
    )
}

/** 使用已有候选创建错误引用，并先完整处理候选以收集诊断状态。 */
fun createErrorReferenceWithExistingCandidate(
    candidate: Candidate,
    diagnostic: ConeDiagnostic,
    source: CjSourceElement?,
    resolutionContext: ResolutionContext,
    resolutionStageRunner: ResolutionStageRunner,
): CfirErrorReferenceWithCandidate {
    resolutionStageRunner.fullyProcessCandidate(candidate, resolutionContext)
    return CfirErrorReferenceWithCandidate(source, candidate.callInfo.name, candidate, diagnostic)
}

/** 创建并完整处理错误候选，用于错误引用保留调用解析上下文。 */
fun ResolutionStageRunner.createErrorCandidate(
    callInfo: CallInfo,
    resolutionContext: ResolutionContext,
    diagnostic: ConeDiagnostic,
): Candidate {
    val candidate = CandidateFactory(resolutionContext, callInfo).createErrorCandidate(callInfo, diagnostic)
    processCandidate(candidate, resolutionContext, stopOnFirstError = false)
    return candidate
}
