package org.cangnova.cangjie.cfir.resolve.calls.overloads

import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate

/**
 * 按顺序组合多个重载冲突消解器。
 *
 * 每个 resolver 都在上一轮保留下来的候选集合上继续收窄，直到只剩一个候选或阶段耗尽。
 */
class ConeCompositeConflictResolver(
    /** 参与组合的冲突消解器序列。 */
    private vararg val conflictResolvers: ConeCallConflictResolver
) : ConeCallConflictResolver() {
    /** 顺序运行内部消解器并返回最终 maximally-specific 候选集合。 */
    override fun chooseMaximallySpecificCandidates(
        candidates: Set<Candidate>,
    ): Set<Candidate> {
        if (candidates.size <= 1) return candidates
        var currentCandidates = candidates
        var index = 0
        while (currentCandidates.size > 1 && index < conflictResolvers.size) {
            val conflictResolver = conflictResolvers[index++]
            currentCandidates = conflictResolver.chooseMaximallySpecificCandidates(currentCandidates)
        }
        return currentCandidates
    }
}
