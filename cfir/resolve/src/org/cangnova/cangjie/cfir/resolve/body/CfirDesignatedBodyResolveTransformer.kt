package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.resolve.CfirResolutionMode
import org.cangnova.cangjie.cfir.scopes.CfirScopeSession
import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * 鎸囧畾璺緞 body resolve transformer銆? *
 * 浠呰В鏋愭寚瀹氱殑澹版槑锛岃烦杩囪矾寰勫鐨勫叾浠栧０鏄庛€? * 鏀寔 file 鈫?(class)? 鈫?declaration 涓夌骇璺緞銆? *
 * 鐢ㄤ簬 [CfirReturnTypeCalculatorWithJump] 瑙﹀彂鐨勬寜闇€瑙ｆ瀽锛? * 褰?IMPLICIT_TYPES 闃舵閬囧埌涓€涓皻鏈В鏋愮殑澹版槑寮曠敤鏃讹紝
 * 閫氳繃 designated transformer 浠呰В鏋愮洰鏍囧０鏄庯紝閬垮厤鍏ㄦ枃浠堕噸瑙ｆ瀽銆? *
 * 鍙傝€?K2 FirDesignatedBodyResolveTransformerForReturnTypeCalculator銆? */
class CfirDesignatedBodyResolveTransformer(
    private val designation: CfirCallableDeclaration,
    session: CfirSession,
    scopeSession: CfirScopeSession,
    implicitBodyResolveComputationSession: CfirImplicitBodyResolveComputationSession,
    returnTypeCalculator: CfirReturnTypeCalculator,
) : CfirImplicitAwareBodyResolveTransformer(
    session = session,
    scopeSession = scopeSession,
    implicitBodyResolveComputationSession = implicitBodyResolveComputationSession,
    phase = CfirResolvePhase.IMPLICIT_TYPES,
    implicitTypeOnly = true,
    returnTypeCalculator = returnTypeCalculator,
) {

    /** 鏈€鍚庝竴娆″彉鎹㈢殑缁撴灉 */
    var lastResult: CfirElement? = null
        private set

    override fun transformDeclarationContent(
        declaration: CfirDeclaration,
        data: CfirResolutionMode,
    ): CfirDeclaration {
        // 浠呭彉鎹㈡寚瀹氱殑澹版槑
        if (declaration === designation) {
            val result = declaration.transform<CfirDeclaration, CfirResolutionMode>(this, data)
            lastResult = result
            return result
        }
        // 璺緞涓婄殑瀹瑰櫒锛坒ile銆乧lass锛夐渶瑕佺户缁亶鍘嗕互寤虹珛 scope 涓婁笅鏂?
        return when (declaration) {
            is CfirFile -> super.transformDeclarationContent(declaration, data)
            is CfirClass -> {
                if (containsDesignation(declaration)) {
                    super.transformDeclarationContent(declaration, data)
                } else {
                    declaration
                }
            }
            else -> declaration // 璺宠繃鏃犲叧澹版槑
        }
    }

    /** 妫€鏌ョ被鏄惁鍖呭惈鎸囧畾鐨勫０鏄?*/
    private fun containsDesignation(klass: CfirClass): Boolean {
        return klass.declarations.any { it === designation }
    }
}

