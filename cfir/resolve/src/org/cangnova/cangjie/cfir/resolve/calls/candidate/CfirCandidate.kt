package org.cangnova.cangjie.cfir.resolve.calls.candidate

import org.cangnova.cangjie.cfir.resolve.calls.CfirTypeSubstitutor
import org.cangnova.cangjie.cfir.resolve.inference.CfirConstraintSystem
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.types.ConeCangjieType

/**
 * 璋冪敤瑙ｆ瀽鍊欓€夛紙Phase 3 鐗堟湰锛夈€? *
 * 灏佽涓€涓€欓€夌鍙峰強鍏跺湪楠岃瘉绠＄嚎涓殑鐘舵€侊細
 * - 鍙傛暟鏄犲皠锛堝疄鍙傗啋褰㈠弬鐨勫搴斿叧绯伙級
 * - 浣跨敤鐨勯粯璁ゅ€煎弬鏁版暟閲? * - 绫诲瀷鍙傛暟鏇挎崲鍣? * - 閫傜敤鎬х瓑绾у拰璇婃柇淇℃伅
 *
 * 瀵归綈 K2 Candidate锛屽幓鎺夌害鏉熺郴缁熴€乸ostponedAtoms銆丼AM 杞崲绛夈€? */
class CfirCandidate(
    /** 鍊欓€夌鍙?*/
    val symbol: CfirCallableSymbol<*>,
    /** 鍏宠仈鐨勮皟鐢ㄤ俊鎭?*/
    val callInfo: CfirCallInfo,
    /** 鍊欓€夋潵婧愮殑 scope锛堢敤浜庨噸杞芥秷姝т腑鐨?override 杩囨护锛?*/
    val originScope: CfirScope? = null,
) {
    /** 瀹炲弬鈫掑舰鍙傛槧灏勶紙鐢?MapArguments 闃舵濉厖锛?*/
    var argumentMapping: Map<Int, Int> = emptyMap()

    /** 浣跨敤鐨勯粯璁ゅ€煎弬鏁版暟閲?*/
    var numDefaults: Int = 0

    /** 绫诲瀷鍙傛暟鏇挎崲鍣紙鏄惧紡绫诲瀷鍙傛暟鐨勬浛鎹級 */
    var substitutor: CfirTypeSubstitutor = CfirTypeSubstitutor.Empty

    /** 绾︽潫绯荤粺锛堟硾鍨嬫帹鏂椂浣跨敤锛孭hase 4锛?*/
    var constraintSystem: CfirConstraintSystem? = null

    /** 褰撳墠鏈€浣庨€傜敤鎬х瓑绾э紙楠岃瘉闃舵涓彇鏈€宸€硷級 */
    var lowestApplicability: CfirCandidateApplicability = CfirCandidateApplicability.RESOLVED

    /** 璇婃柇淇℃伅鍒楄〃 */
    val diagnostics: MutableList<CfirResolutionDiagnostic> = mutableListOf()

    /** 娣诲姞璇婃柇锛屽悓鏃舵洿鏂?lowestApplicability */
    fun addDiagnostic(diagnostic: CfirResolutionDiagnostic) {
        diagnostics.add(diagnostic)
        if (diagnostic.applicability < lowestApplicability) {
            lowestApplicability = diagnostic.applicability
        }
    }

    /** 鏄惁涓烘垚鍔熷€欓€?*/
    val isSuccessful: Boolean
        get() = lowestApplicability.isSuccess

    /**
     * 浠庡€欓€夌鍙蜂腑鎻愬彇鏇挎崲鍚庣殑杩斿洖绫诲瀷銆?     *
     * 鍏堜粠绗﹀彿澹版槑鑾峰彇杩斿洖绫诲瀷锛屽啀閫氳繃 substitutor 鏇挎崲绫诲瀷鍙傛暟銆?     */
    fun resolvedReturnType(): ConeCangjieType? {
        if (!symbol.isBound) return null
        val decl = symbol.cfir
        val typeRef = when (decl) {
            is org.cangnova.cangjie.cfir.declarations.CfirFunction -> decl.returnTypeRef
            is org.cangnova.cangjie.cfir.declarations.CfirProperty -> decl.returnTypeRef
            is org.cangnova.cangjie.cfir.declarations.CfirConstructor -> decl.returnTypeRef
            else -> return null
        }
        val coneType = (typeRef as? org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef)?.coneType
            ?: return null
        return substitutor.substituteOrSelf(coneType)
    }
}

