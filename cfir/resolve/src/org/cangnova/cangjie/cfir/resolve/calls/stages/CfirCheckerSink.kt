package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidate
import org.cangnova.cangjie.cfir.semantics.ResolutionDiagnostic

/**
 * 诊断接收器接口。
 * 验证阶段通过它上报诊断；具体实现负责把诊断累积到候选上，
 * 并根据 `stopOnFirstError` 策略决定是否终止后续阶段。
 * 对齐 K2 `CheckerSink`，但改为同步 `shouldStop` 判定。
 */
interface CfirCheckerSink {

    /** 上报一个诊断。 */
    fun reportDiagnostic(diagnostic: ResolutionDiagnostic)

    /** 是否应停止后续验证阶段。 */
    val shouldStop: Boolean
}

/**
 * [CfirCheckerSink] 的标准实现。
 * 它会把诊断累积到关联候选上；当 `stopOnFirstError=true`
 * 且候选已失败时，后续阶段可据此提前退出。
 */
class CfirCheckerSinkImpl(
    private val candidate: CfirCandidate,
    private val stopOnFirstError: Boolean = true,
) : CfirCheckerSink {

    override fun reportDiagnostic(diagnostic: ResolutionDiagnostic) {
        candidate.addDiagnostic(diagnostic)
    }

    override val shouldStop: Boolean
        get() = stopOnFirstError && !candidate.isSuccessful
}

