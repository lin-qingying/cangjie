package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol

/**
 * 候选收集器，负责在 scope 塔遍历过程中收集和排序候选。
 *
 * 跟踪当前最佳候选的适用性等级和塔层级，
 * 当发现更优候选时清除劣质候选。
 *
 * Phase 2 为简化骨架，后续将添加塔层级比较、适用性排序等。
 *
 * 参考 K2 CandidateCollector(components, resolutionStageRunner)。
 */
class CfirCandidateCollector(
    val components: CfirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
    private val resolutionStageRunner: CfirResolutionStageRunner,
) {

    private val candidates = mutableListOf<CfirCandidate>()

    var currentApplicability: CfirCandidateApplicability = CfirCandidateApplicability.HIDDEN
        private set

    /** 收集一个候选，通过 resolutionStageRunner 验证 */
    fun consumeCandidate(candidate: CfirCandidate): CfirCandidateApplicability {
        val applicability = resolutionStageRunner.processCandidate(candidate)
        candidate.applicability = applicability

        if (applicability.ordinal >= currentApplicability.ordinal) {
            if (applicability.ordinal > currentApplicability.ordinal) {
                // 发现更优候选，清除之前的
                candidates.clear()
                currentApplicability = applicability
            }
            candidates.add(candidate)
        }
        return applicability
    }

    /** 返回最佳候选列表 */
    fun bestCandidates(): List<CfirCandidate> = candidates.toList()

    /** 是否已找到成功候选 */
    val isSuccess: Boolean
        get() = currentApplicability == CfirCandidateApplicability.RESOLVED && candidates.isNotEmpty()

    /** 重置状态，用于新一轮收集 */
    fun newDataSet() {
        candidates.clear()
        currentApplicability = CfirCandidateApplicability.HIDDEN
    }
}
