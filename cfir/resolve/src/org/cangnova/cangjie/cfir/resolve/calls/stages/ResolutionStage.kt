package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CheckerSink

/**
 * 候选验证阶段的抽象基类。
 * 每个阶段只关注候选的一个方面，例如可见性、参数映射或参数类型，
 * 并通过 [CfirCheckerSink] 上报诊断。
 * Phase 3 使用同步 API；当前仓颉实现暂不需要 postpone / resume。
 * 对齐 K2 `ResolutionStage`。
 */
abstract class ResolutionStage {
    context(sink: CheckerSink, context: ResolutionContext)
    abstract suspend fun check(candidate: Candidate)
}