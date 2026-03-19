package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidate

/**
 * 候选验证阶段的抽象基类。
 * 每个阶段只关注候选的一个方面，例如可见性、参数映射或参数类型，
 * 并通过 [CfirCheckerSink] 上报诊断。
 * Phase 3 使用同步 API；当前仓颉实现暂不需要 postpone / resume。
 * 对齐 K2 `ResolutionStage`。
 */
abstract class CfirResolutionStage {

    /**
     * 检查候选是否通过当前阶段的验证。
     * @param candidate 待验证的候选
     * @param sink 诊断接收器
     * @param context 解析上下文
     */
    abstract fun check(
        candidate: CfirCandidate,
        sink: CfirCheckerSink,
        context: CfirResolutionContext,
    )
}

