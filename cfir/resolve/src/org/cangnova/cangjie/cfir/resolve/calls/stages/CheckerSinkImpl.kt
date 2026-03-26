package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CheckerSink
import org.cangnova.cangjie.cfir.semantics.ResolutionDiagnostic
import org.cangnova.cangjie.cfir.semantics.isSuccess
import kotlin.coroutines.Continuation

class CheckerSinkImpl(
    private val candidate: Candidate,
    var continuation: Continuation<Unit>? = null,

    private val stopOnFirstError: Boolean = false,
) : CheckerSink() {


    override val needYielding: Boolean
        get() = stopOnFirstError && !candidate.isSuccessful
    override fun reportDiagnostic(diagnostic: ResolutionDiagnostic) {
        candidate.addDiagnostic(diagnostic)
    }

    override suspend fun yield() {
        kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn {
            continuation = it
            kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
        }
    }
}
