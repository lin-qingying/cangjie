package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidate
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.tower.CfirTowerGroup
import org.cangnova.cangjie.cfir.semantics.CandidateApplicability

/**
 * 候选收集器，负责在 scope 塔遍历过程中收集并排序候选。
 * 它会持续跟踪当前最优的适用性等级与 tower 层级，
 * 并支持在满足条件时提前停止后续 tower 遍历。
 */
class CfirCandidateCollector(
    val components: CfirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
    private val resolutionStageRunner: CfirResolutionStageRunner,
) {

    private val candidates = mutableListOf<CfirCandidate>()

    var currentApplicability: CandidateApplicability = CandidateApplicability.HIDDEN
        private set

    /** 当前最优候选所在的 tower 层级。 */
    var bestGroup: CfirTowerGroup? = null
        private set

    /**
     * 收集一个候选，并通过 `resolutionStageRunner` 执行验证。
     * @param group 候选来源的 tower 层级
     * @param candidate 待收集的候选
     * @param context 解析上下文
     */
    fun consumeCandidate(
        group: CfirTowerGroup,
        candidate: CfirCandidate,
        context: CfirResolutionContext,
    ): CandidateApplicability {
        val applicability = resolutionStageRunner.processCandidate(candidate, context)

        val currentBest = bestGroup
        if (currentBest == null || group < currentBest) {
            // 更优 tower 层级出现时，清空旧候选
            candidates.clear()
            bestGroup = group
            currentApplicability = applicability
            candidates.add(candidate)
        } else if (group == currentBest) {
            // 同层级内按适用性比较
            if (applicability.ordinal >= currentApplicability.ordinal) {
                if (applicability.ordinal > currentApplicability.ordinal) {
                    candidates.clear()
                    currentApplicability = applicability
                }
                candidates.add(candidate)
            }
        }
        // 当前 group 比最优层级更差时直接忽略

        return applicability
    }

    /** 返回当前最优候选列表。 */
    fun bestCandidates(): List<CfirCandidate> = candidates.toList()

    /** 是否已经找到成功候选。 */
    val isSuccess: Boolean
        get() = currentApplicability.isSuccess && candidates.isNotEmpty()

    /**
     * 判断是否应在当前 group 结束后停止 tower 遍历。
     */
    fun shouldStopAtTheGroup(group: CfirTowerGroup): Boolean {
        val currentBest = bestGroup ?: return false
        if (!currentApplicability.shouldStopResolve) return false
        return group > currentBest
    }

    /** 重置内部状态，用于新一轮收集。 */
    fun newDataSet() {
        candidates.clear()
        currentApplicability = CandidateApplicability.HIDDEN
        bestGroup = null
    }
}

