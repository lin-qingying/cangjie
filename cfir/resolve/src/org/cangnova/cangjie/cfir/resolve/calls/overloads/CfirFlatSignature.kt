package org.cangnova.cangjie.cfir.resolve.calls.overloads

import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidate
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangjieType

/**
 * 扁平化函数签名，用于重载消歧中的特化度比较。
 *
 * 提取候选的关键信息用于逐参数位置比较。
 *
 * 对齐 K2 FlatSignature。
 */
class CfirFlatSignature(
    /** 原始候选 */
    val origin: CfirCandidate,
    /** 类型参数列表 */
    val typeParameters: List<CfirTypeParameter>,
    /** 值参数类型列表 */
    val valueParameterTypes: List<ConeCangjieType?>,
    /** 使用的默认值参数数量 */
    val numDefaults: Int,
) {
    /** 是否为泛型函数 */
    val isGeneric: Boolean get() = typeParameters.isNotEmpty()

    companion object {
        /** 从候选构建 FlatSignature */
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
