package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidate

/**
 * 鍊欓€夐獙璇侀樁娈垫娊璞″熀绫汇€? *
 * 姣忎釜闃舵妫€鏌ュ€欓€夌殑鏌愪竴鏂归潰锛堝彲瑙佹€с€佸弬鏁版槧灏勩€佸弬鏁扮被鍨嬬瓑锛夛紝
 * 閫氳繃 [CfirCheckerSink] 鎶ュ憡璇婃柇銆? *
 * Phase 3 浣跨敤鍚屾 API锛圞2 浣跨敤 suspend锛夛紝鍥犱负浠撻涓嶉渶瑕?postpone/resume銆? *
 * 瀵归綈 K2 ResolutionStage銆? */
abstract class CfirResolutionStage {

    /**
     * 妫€鏌ュ€欓€夋槸鍚﹂€氳繃褰撳墠闃舵鐨勯獙璇併€?     *
     * @param candidate 寰呴獙璇佺殑鍊欓€?     * @param sink 璇婃柇鎶ュ憡鎺ユ敹鍣?     * @param context 瑙ｆ瀽涓婁笅鏂?     */
    abstract fun check(
        candidate: CfirCandidate,
        sink: CfirCheckerSink,
        context: CfirResolutionContext,
    )
}

