package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidateApplicability
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.tower.CfirTowerGroup

/**
 * 鍊欓€夋敹闆嗗櫒锛岃礋璐ｅ湪 scope 濉旈亶鍘嗚繃绋嬩腑鏀堕泦鍜屾帓搴忓€欓€夈€? *
 * 璺熻釜褰撳墠鏈€浣冲€欓€夌殑閫傜敤鎬х瓑绾у拰濉斿眰绾э細
 * - 鏇翠紭 TowerGroup 鏃舵竻闄ゆ棫鍊欓€? * - 鍚?TowerGroup 涓寜閫傜敤鎬ф帓搴? * - 鏀寔 shouldStopAtTheGroup 鎻愬墠缁堟 Tower 閬嶅巻
 *
 * 瀵归綈 K2 CandidateCollector(components, resolutionStageRunner)銆? */
class CfirCandidateCollector(
    val components: CfirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
    private val resolutionStageRunner: CfirResolutionStageRunner,
) {

    private val candidates = mutableListOf<CfirCandidate>()

    var currentApplicability: CfirCandidateApplicability = CfirCandidateApplicability.HIDDEN
        private set

    /** 褰撳墠鏈€浣冲€欓€夌殑 TowerGroup */
    var bestGroup: CfirTowerGroup? = null
        private set

    /**
     * 鏀堕泦涓€涓€欓€夛紝閫氳繃 resolutionStageRunner 楠岃瘉銆?     *
     * @param group 鍊欓€夋潵婧愮殑 Tower 灞傜骇
     * @param candidate 寰呮敹闆嗙殑鍊欓€?     * @param context 瑙ｆ瀽涓婁笅鏂?     */
    fun consumeCandidate(
        group: CfirTowerGroup,
        candidate: CfirCandidate,
        context: CfirResolutionContext,
    ): CfirCandidateApplicability {
        val applicability = resolutionStageRunner.processCandidate(candidate, context)

        val currentBest = bestGroup
        if (currentBest == null || group < currentBest) {
            // 鏇翠紭 Tower 灞傜骇 鈥?娓呴櫎鏃у€欓€?
            candidates.clear()
            bestGroup = group
            currentApplicability = applicability
            candidates.add(candidate)
        } else if (group == currentBest) {
            // 鍚屼竴灞傜骇 鈥?鎸夐€傜敤鎬ф瘮杈?
            if (applicability.ordinal >= currentApplicability.ordinal) {
                if (applicability.ordinal > currentApplicability.ordinal) {
                    candidates.clear()
                    currentApplicability = applicability
                }
                candidates.add(candidate)
            }
        }
        // group > currentBest 鈫?蹇界暐锛堝姡璐ㄥ眰绾э級

        return applicability
    }

    /** 杩斿洖鏈€浣冲€欓€夊垪琛?*/
    fun bestCandidates(): List<CfirCandidate> = candidates.toList()

    /** 鏄惁宸叉壘鍒版垚鍔熷€欓€?*/
    val isSuccess: Boolean
        get() = currentApplicability.isSuccess && candidates.isNotEmpty()

    /**
     * 鏄惁搴斿湪褰撳墠 group 鍋滄 Tower 閬嶅巻銆?     *
     * 褰撳凡鏈夋垚鍔熷€欓€変笖寰呮煡璇㈢殑 group 姣斿綋鍓嶆渶浣?group 鏇村樊鏃惰繑鍥?true銆?     */
    fun shouldStopAtTheGroup(group: CfirTowerGroup): Boolean {
        val currentBest = bestGroup ?: return false
        if (!currentApplicability.shouldStopResolve) return false
        return group > currentBest
    }

    /** 閲嶇疆鐘舵€侊紝鐢ㄤ簬鏂颁竴杞敹闆?*/
    fun newDataSet() {
        candidates.clear()
        currentApplicability = CfirCandidateApplicability.HIDDEN
        bestGroup = null
    }
}

