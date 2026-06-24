package org.cangnova.cangjie.cfir.resolve.calls.candidate

import org.cangnova.cangjie.cfir.semantics.ResolutionDiagnostic

/** 候选检查阶段用于报告诊断和控制 coroutine yield 的 sink。 */
abstract class CheckerSink {
    /** 向当前候选报告解析诊断。 */
    abstract fun reportDiagnostic(diagnostic: ResolutionDiagnostic)

    /** 当前阶段是否需要暂停并把控制权交还给 runner。 */
    abstract val needYielding: Boolean


    /** 暂停当前阶段执行，交还给 [ResolutionStageRunner]。 */
    abstract suspend fun yield()
}

/** 在需要 yield 时暂停当前候选检查阶段。 */
suspend inline fun CheckerSink.yieldIfNeed() {
    if (needYielding) {
        yield()
    }
}

/** 报告诊断后按 sink 策略决定是否 yield。 */
suspend inline fun CheckerSink.yieldDiagnostic(diagnostic: ResolutionDiagnostic) {
    reportDiagnostic(diagnostic)
    yieldIfNeed()
}
