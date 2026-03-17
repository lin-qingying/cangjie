package org.cangnova.cangjie.cfir.resolve.calls.overloads

import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidate

/**
 * 璋冪敤鍐茬獊瑙ｆ瀽鍣ㄦ娊璞″熀绫汇€? *
 * 浠庝竴缁勯€氳繃楠岃瘉鐨勫€欓€変腑閫夋嫨鏈€鐗瑰畾鐨勫€欓€夐泦鍚堛€? * 鐞嗘兂鎯呭喌涓嬭繑鍥炲崟涓€鍊欓€夛紝澶氬€欓€夎〃绀烘涔夈€? *
 * 瀵归綈 K2 ConeCallConflictResolver銆? */
abstract class CfirCallConflictResolver {

    /**
     * 浠庡€欓€夐泦鍚堜腑閫夋嫨鏈€鐗瑰畾鐨勫€欓€夈€?     *
     * @param candidates 閫氳繃楠岃瘉绠＄嚎鐨勫€欓€夐泦鍚?     * @return 鏈€鐗瑰畾鐨勫€欓€夐泦鍚堬紙鍗曚竴 = 鎴愬姛锛屽涓?= 姝т箟锛?     */
    abstract fun chooseMaximallySpecificCandidates(
        candidates: Set<CfirCandidate>,
    ): Set<CfirCandidate>

    /** 渚挎嵎鏂规硶锛氭帴鍙?Collection */
    fun chooseMaximallySpecificCandidates(
        candidates: Collection<CfirCandidate>,
    ): Set<CfirCandidate> = chooseMaximallySpecificCandidates(candidates.toSet())
}

