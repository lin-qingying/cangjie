/*
 * Copyright 2010-2026. cangjie.
 */

package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.explicitTypeRefResolver
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef

/**
 * 绫诲瀷寮曠敤瑙ｆ瀽濮旀墭鍣紙TYPES 闃舵锛夈€? *
 * 妗ユ帴鏍戦亶鍘嗕笌绫诲瀷瑙ｆ瀽锛? * - [CfirTypeResolveTransformer] 璐熻矗鏍戦亶鍘嗗拰 scope 绠＄悊
 * - 鏈被璐熻矗灏嗗崟涓被鍨嬪紩鐢ㄥ鎵樼粰 [CfirExplicitTypeRefResolver] 杩涜瑙ｆ瀽
 * - [CfirExplicitTypeRefResolver] 璐熻矗鍏蜂綋鐨勭被鍨嬭В鏋愰€昏緫
 *
 * data 鍙傛暟涓哄綋鍓嶄綔鐢ㄥ煙鍐呭彲瑙佺殑绫诲瀷鍙傛暟鏄犲皠 `Map<String, CfirTypeParameter>`銆? *
 * 瀵归綈 K2: `FirSpecificTypeResolverTransformer`
 */
class CfirSpecificTypeResolverTransformer(
    override val session: CfirSession,
) : CfirAbstractTreeTransformer<Map<String, CfirTypeParameter>>(CfirResolvePhase.TYPES) {

    /** 浠?session 鑾峰彇缁熶竴娉ㄥ唽鐨勬樉寮忕被鍨嬭В鏋愬櫒銆?*/
    private val explicitTypeRefResolver = session.explicitTypeRefResolver

    override fun transformTypeRef(
        typeRef: CfirTypeRef,
        data: Map<String, CfirTypeParameter>,
    ): CfirTypeRef {
        return explicitTypeRefResolver.resolveExplicitTypeRef(typeRef, data)
    }

    override fun transformResolvedTypeRef(
        resolvedTypeRef: CfirResolvedTypeRef,
        data: Map<String, CfirTypeParameter>,
    ): CfirTypeRef {
        // 宸茶В鏋?鈫?鐩存帴杩斿洖
        return resolvedTypeRef
    }

    override fun transformImplicitTypeRef(
        implicitTypeRef: CfirImplicitTypeRef,
        data: Map<String, CfirTypeParameter>,
    ): CfirTypeRef {
        // 闅愬紡绫诲瀷 鈫?璺宠繃锛岀暀缁?
        // IMPLICIT_TYPES 闃舵澶勭悊
        return implicitTypeRef
    }
}

