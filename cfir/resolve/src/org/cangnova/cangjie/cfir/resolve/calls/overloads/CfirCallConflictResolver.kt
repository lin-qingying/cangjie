package org.cangnova.cangjie.cfir.resolve.calls.overloads

import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidate

/**
 * 调用冲突解析器抽象基类。
 *
 * 从一组通过验证的候选中选择最特定的候选集合。
 * 理想情况下返回单一候选，多候选表示歧义。
 *
 * 对齐 K2 ConeCallConflictResolver。
 */
abstract class CfirCallConflictResolver {

    /**
     * 从候选集合中选择最特定的候选。
     *
     * @param candidates 通过验证管线的候选集合
     * @return 最特定的候选集合（单一 = 成功，多个 = 歧义）
     */
    abstract fun chooseMaximallySpecificCandidates(
        candidates: Set<CfirCandidate>,
    ): Set<CfirCandidate>

    /** 便捷方法：接受 Collection */
    fun chooseMaximallySpecificCandidates(
        candidates: Collection<CfirCandidate>,
    ): Set<CfirCandidate> = chooseMaximallySpecificCandidates(candidates.toSet())
}
