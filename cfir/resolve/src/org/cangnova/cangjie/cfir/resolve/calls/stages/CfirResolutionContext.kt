package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.resolve.body.CfirBodyResolveContext
import org.cangnova.cangjie.cfir.resolve.inference.CfirInferenceComponents
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.ConeSubtypeChecker

/**
 * 瑙ｆ瀽涓婁笅鏂囷紝涓洪獙璇侀樁娈垫彁渚涙墍闇€鐨勭幆澧冧俊鎭€? *
 * 瀵归綈 K2 ResolutionContext锛堝幓鎺?BodyResolveComponents 渚濊禆锛屼粎鏆撮湶蹇呰鏈嶅姟锛夈€? */
class CfirResolutionContext(
    /** 缂栬瘧鍣?session */
    val session: CfirSession,
    /** Body 瑙ｆ瀽涓婁笅鏂?*/
    val bodyResolveContext: CfirBodyResolveContext,
    /** 瀛愮被鍨嬫鏌ュ櫒 */
    val subtypeChecker: ConeSubtypeChecker,
    /** 鎺ㄦ柇缁勪欢锛圥hase 4 娉涘瀷鎺ㄦ柇锛?*/
    val inferenceComponents: CfirInferenceComponents? = null,
)

