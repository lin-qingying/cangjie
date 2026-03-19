package org.cangnova.cangjie.cfir.resolve.calls.overloads

import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidate

/**
 * 调用冲突解析器的抽象基类。
 * 它从一组已经通过验证的候选中选出“最特定”的候选集合。
 * 理想情况下返回单个候选；若返回多个，则表示仍然存在歧义。
 * 对齐 K2 `ConeCallConflictResolver`。
 */
abstract class CfirCallConflictResolver {

    /**
     * 从候选集合中选出最特定的候选。
     * @param candidates 已通过验证管线的候选集合
     * @return 最特定的候选集合；单元素表示成功，多元素表示歧义
     */
    abstract fun chooseMaximallySpecificCandidates(
        candidates: Set<CfirCandidate>,
    ): Set<CfirCandidate>

    /** 便捷重载，接受 `Collection`。 */
    fun chooseMaximallySpecificCandidates(
        candidates: Collection<CfirCandidate>,
    ): Set<CfirCandidate> = chooseMaximallySpecificCandidates(candidates.toSet())
}

