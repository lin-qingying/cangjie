package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CheckerSink

class CollectionLiteralOuterCandidateContext(
    /**
     * [Candidate] whose constraint system must be expanded by the CL's system.
     * During overload resolution, it is always the immediate containing candidate.
     * During completion, it may be an arbitrary outer call.
     */
    val containingCandidate: Candidate,
    /**
     * [CheckerSink] of outer candidate.
     * Only non-`null` when CL is expanded as part of the overload resolution of some outer call.
     */
    val checkerSink: CheckerSink? = null,
)
