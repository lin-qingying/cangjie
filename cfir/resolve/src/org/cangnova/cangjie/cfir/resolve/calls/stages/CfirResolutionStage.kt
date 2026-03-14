package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidate

/**
 * 候选验证阶段抽象基类。
 *
 * 每个阶段检查候选的某一方面（可见性、参数映射、参数类型等），
 * 通过 [CfirCheckerSink] 报告诊断。
 *
 * Phase 3 使用同步 API（K2 使用 suspend），因为仓颉不需要 postpone/resume。
 *
 * 对齐 K2 ResolutionStage。
 */
abstract class CfirResolutionStage {

    /**
     * 检查候选是否通过当前阶段的验证。
     *
     * @param candidate 待验证的候选
     * @param sink 诊断报告接收器
     * @param context 解析上下文
     */
    abstract fun check(
        candidate: CfirCandidate,
        sink: CfirCheckerSink,
        context: CfirResolutionContext,
    )
}
