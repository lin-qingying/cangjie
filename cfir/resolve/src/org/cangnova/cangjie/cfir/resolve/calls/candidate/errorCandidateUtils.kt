package org.cangnova.cangjie.cfir.resolve.calls.candidate

import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.cfir.resolve.body.ResolutionStageRunner
import org.cangnova.cangjie.cfir.resolve.body.fullyProcessCandidate
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.source.CjSourceElement

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

fun ResolutionStageRunner.createErrorCandidate(
    callInfo: CallInfo,
    resolutionContext: ResolutionContext,
    diagnostic: ConeDiagnostic,
): Candidate {
    val candidate = CandidateFactory(resolutionContext).createErrorCandidate(callInfo, diagnostic)
    processCandidate(candidate, resolutionContext, stopOnFirstError = false)
    return candidate
}
