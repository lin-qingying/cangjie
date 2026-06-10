package org.cangnova.cangjie.cfir.resolve.calls.candidate

import org.cangnova.cangjie.cfir.semantics.ResolutionDiagnostic

abstract class CheckerSink {
    abstract fun reportDiagnostic(diagnostic: ResolutionDiagnostic)

    abstract val needYielding: Boolean


    abstract suspend fun yield()
}

suspend inline fun CheckerSink.yieldIfNeed() {
    if (needYielding) {
        yield()
    }
}

suspend inline fun CheckerSink.yieldDiagnostic(diagnostic: ResolutionDiagnostic) {
    reportDiagnostic(diagnostic)
    yieldIfNeed()
}
