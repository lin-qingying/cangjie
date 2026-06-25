package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CheckerSink
import org.cangnova.cangjie.cfir.semantics.ResolutionDiagnostic
import org.cangnova.cangjie.cfir.semantics.isSuccess
import kotlin.coroutines.Continuation

/** 默认 checker sink 实现，把诊断写入候选并按需暂停阶段 coroutine。 */
class CheckerSinkImpl(
    /** 当前正在检查的候选。 */
    private val candidate: Candidate,
    /** 当前挂起的 stage coroutine continuation。 */
    var continuation: Continuation<Unit>? = null,

    /** 是否在首个错误后暂停候选处理。 */
    private val stopOnFirstError: Boolean = false,
) : CheckerSink() {


    /** 当前 sink 是否需要暂停后续阶段执行。 */
    override val needYielding: Boolean
        get() = stopOnFirstError && !candidate.isSuccessful
    /** 将诊断追加到候选。 */
    override fun reportDiagnostic(diagnostic: ResolutionDiagnostic) {
        candidate.addDiagnostic(diagnostic)
    }

    /** 保存当前 continuation 并以 coroutine suspended 标记暂停执行。 */
    override suspend fun yield() {
        kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn {
            continuation = it
            kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
        }
    }
}
