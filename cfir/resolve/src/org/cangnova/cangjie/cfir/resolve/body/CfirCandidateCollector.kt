package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidateApplicability
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.tower.CfirTowerGroup

/**
 * 候选收集器，负责在 scope 塔遍历过程中收集和排序候选。
 *
 * 跟踪当前最佳候选的适用性等级和塔层级：
 * - 更优 TowerGroup 时清除旧候选
 * - 同 TowerGroup 中按适用性排序
 * - 支持 shouldStopAtTheGroup 提前终止 Tower 遍历
 *
 * 对齐 K2 CandidateCollector(components, resolutionStageRunner)。
 */
class CfirCandidateCollector(
    val components: CfirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
    private val resolutionStageRunner: CfirResolutionStageRunner,
) {

    private val candidates = mutableListOf<CfirCandidate>()

    var currentApplicability: CfirCandidateApplicability = CfirCandidateApplicability.HIDDEN
        private set

    /** 当前最佳候选的 TowerGroup */
    var bestGroup: CfirTowerGroup? = null
        private set

    /**
     * 收集一个候选，通过 resolutionStageRunner 验证。
     *
     * @param group 候选来源的 Tower 层级
     * @param candidate 待收集的候选
     * @param context 解析上下文
     */
    fun consumeCandidate(
        group: CfirTowerGroup,
        candidate: CfirCandidate,
        context: CfirResolutionContext,
    ): CfirCandidateApplicability {
        val applicability = resolutionStageRunner.processCandidate(candidate, context)

        val currentBest = bestGroup
        if (currentBest == null || group < currentBest) {
            // 更优 Tower 层级 — 清除旧候选
            candidates.clear()
            bestGroup = group
            currentApplicability = applicability
            candidates.add(candidate)
        } else if (group == currentBest) {
            // 同一层级 — 按适用性比较
            if (applicability.ordinal >= currentApplicability.ordinal) {
                if (applicability.ordinal > currentApplicability.ordinal) {
                    candidates.clear()
                    currentApplicability = applicability
                }
                candidates.add(candidate)
            }
        }
        // group > currentBest → 忽略（劣质层级）

        return applicability
    }

    /** 返回最佳候选列表 */
    fun bestCandidates(): List<CfirCandidate> = candidates.toList()

    /** 是否已找到成功候选 */
    val isSuccess: Boolean
        get() = currentApplicability.isSuccess && candidates.isNotEmpty()

    /**
     * 是否应在当前 group 停止 Tower 遍历。
     *
     * 当已有成功候选且待查询的 group 比当前最佳 group 更差时返回 true。
     */
    fun shouldStopAtTheGroup(group: CfirTowerGroup): Boolean {
        val currentBest = bestGroup ?: return false
        if (!currentApplicability.shouldStopResolve) return false
        return group > currentBest
    }

    /** 重置状态，用于新一轮收集 */
    fun newDataSet() {
        candidates.clear()
        currentApplicability = CfirCandidateApplicability.HIDDEN
        bestGroup = null
    }
}
