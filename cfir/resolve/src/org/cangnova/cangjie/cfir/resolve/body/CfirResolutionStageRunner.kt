package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidateApplicability
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirCheckerSinkImpl
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirResolutionContext

/**
 * 鍊欓€夐獙璇佺绾挎墽琛屽櫒銆? *
 * 瀵硅皟鐢ㄨВ鏋愪腑鐨勬瘡涓€欓€夛紙CfirCandidate锛夋寜鍏?callKind 鐨?resolutionSequence
 * 椤哄簭鎵ц楠岃瘉闃舵锛岄€氳繃 CfirCheckerSinkImpl 鏀堕泦璇婃柇锛? * 鏀寔 stopOnFirstError 鎻愬墠閫€鍑恒€? *
 * 瀵归綈 K2 ResolutionStageRunner銆? */
class CfirResolutionStageRunner {

    /**
     * 瀵瑰€欓€夋墽琛岄獙璇佺绾裤€?     *
     * 閬嶅巻 candidate.callInfo.callKind.resolutionSequence 涓殑姣忎釜闃舵锛?     * 鍚勯樁娈甸€氳繃 sink 鎶ュ憡璇婃柇骞跺彲鑳借Е鍙戞彁鍓嶉€€鍑恒€?     *
     * @return 鍊欓€夐€氳繃绠＄嚎鍚庣殑鏈€缁堥€傜敤鎬х瓑绾?     */
    fun processCandidate(
        candidate: CfirCandidate,
        context: CfirResolutionContext,
        stopOnFirstError: Boolean = true,
    ): CfirCandidateApplicability {
        val sink = CfirCheckerSinkImpl(candidate, stopOnFirstError)
        val stages = candidate.callInfo.callKind.resolutionSequence

        for (stage in stages) {
            if (sink.shouldStop) break
            stage.check(candidate, sink, context)
        }

        return candidate.lowestApplicability
    }
}

