package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CheckerSink

/**
 * 为变量/名称访问调用初始化空参数映射的解析阶段。
 *
 * 对齐 Kotlin FIR `InitializeEmptyArgumentMap`：变量访问候选仍会参与 completion 与 postponed atom 遍历，
 * 因此即使调用没有参数检查，也必须初始化 argument mapping。
 */
object CfirInitializeEmptyArgumentMap : ResolutionStage() {
    context(sink: CheckerSink, context: ResolutionContext)
    /** 给无参数候选写入空参数列表与空 argument mapping。 */
    override suspend fun check(candidate: Candidate) {
        candidate.initializeArgumentMapping(arguments = emptyList(), argumentMapping = linkedMapOf())
    }
}
