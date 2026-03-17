package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidateApplicability
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirResolutionDiagnostic

/**
 * 璇婃柇鎶ュ憡鎺ユ敹鍣ㄦ帴鍙ｃ€? *
 * 楠岃瘉闃舵閫氳繃姝ゆ帴鍙ｆ姤鍛婅瘖鏂紝瀹炵幇绫昏礋璐ｅ皢璇婃柇绱Н鍒板€欓€変笂锛? * 骞舵牴鎹?stopOnFirstError 绛栫暐鍐冲畾鏄惁缁堟鍚庣画闃舵銆? *
 * 瀵归綈 K2 CheckerSink锛堝幓鎺?suspend yield 鏈哄埗锛屾敼鐢ㄥ悓姝?shouldStop 鍒ゅ畾锛夈€? */
interface CfirCheckerSink {

    /** 鎶ュ憡涓€涓瘖鏂?*/
    fun reportDiagnostic(diagnostic: CfirResolutionDiagnostic)

    /** 鏄惁搴斿仠姝㈠悗缁獙璇侀樁娈?*/
    val shouldStop: Boolean
}

/**
 * [CfirCheckerSink] 鐨勬爣鍑嗗疄鐜般€? *
 * 灏嗚瘖鏂疮绉埌鍏宠仈鐨勫€欓€変笂锛屽綋 stopOnFirstError=true 涓斿€欓€夊凡澶辫触鏃舵爣璁板仠姝€? */
class CfirCheckerSinkImpl(
    private val candidate: CfirCandidate,
    private val stopOnFirstError: Boolean = true,
) : CfirCheckerSink {

    override fun reportDiagnostic(diagnostic: CfirResolutionDiagnostic) {
        candidate.addDiagnostic(diagnostic)
    }

    override val shouldStop: Boolean
        get() = stopOnFirstError && !candidate.isSuccessful
}

