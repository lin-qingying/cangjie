package org.cangnova.cangjie.cfir.resolve.calls.overloads

import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidate
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangjieType

/**
 * 鎵佸钩鍖栧嚱鏁扮鍚嶏紝鐢ㄤ簬閲嶈浇娑堟涓殑鐗瑰寲搴︽瘮杈冦€? *
 * 鎻愬彇鍊欓€夌殑鍏抽敭淇℃伅鐢ㄤ簬閫愬弬鏁颁綅缃瘮杈冦€? *
 * 瀵归綈 K2 FlatSignature銆? */
class CfirFlatSignature(
    /** 鍘熷鍊欓€?*/
    val origin: CfirCandidate,
    /** 绫诲瀷鍙傛暟鍒楄〃 */
    val typeParameters: List<CfirTypeParameter>,
    /** 鍊煎弬鏁扮被鍨嬪垪琛?*/
    val valueParameterTypes: List<ConeCangjieType?>,
    /** 浣跨敤鐨勯粯璁ゅ€煎弬鏁版暟閲?*/
    val numDefaults: Int,
) {
    /** 鏄惁涓烘硾鍨嬪嚱鏁?*/
    val isGeneric: Boolean get() = typeParameters.isNotEmpty()

    companion object {
        /** 浠庡€欓€夋瀯寤?FlatSignature */
        fun create(candidate: CfirCandidate): CfirFlatSignature {
            val symbol = candidate.symbol
            if (!symbol.isBound) {
                return CfirFlatSignature(candidate, emptyList(), emptyList(), 0)
            }

            val decl = symbol.cfir
            val typeParams: List<CfirTypeParameter>
            val paramTypes: List<ConeCangjieType?>

            when (decl) {
                is CfirFunction -> {
                    typeParams = decl.typeParameters
                    paramTypes = decl.valueParameters.map { vp ->
                        (vp.returnTypeRef as? CfirResolvedTypeRef)?.coneType
                    }
                }
                is CfirConstructor -> {
                    typeParams = decl.typeParameters
                    paramTypes = decl.valueParameters.map { vp ->
                        (vp.returnTypeRef as? CfirResolvedTypeRef)?.coneType
                    }
                }
                else -> {
                    typeParams = emptyList()
                    paramTypes = emptyList()
                }
            }

            return CfirFlatSignature(candidate, typeParams, paramTypes, candidate.numDefaults)
        }
    }
}

