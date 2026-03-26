package org.cangnova.cangjie.cfir.resolve.calls.overloads

import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType

/**
 * 扁平化函数签名，用于重载消歧时的特化程度比较。
 * 它提取候选上的关键信息，便于按参数位置逐项比较。
 * 对齐 K2 `FlatSignature`。
 */
class CfirFlatSignature(
    /** 原始候选。 */
    val origin: Candidate,
    /** 类型参数列表。 */
    val typeParameters: List<CfirTypeParameter>,
    /** 值参数类型列表。 */
    val valueParameterTypes: List<ConeCangJieType?>,
    /** 使用的默认值参数个数。 */
    val numDefaults: Int,
    /** 是否使用了 QuestTy 回退。 */
    val usedQuestFallback: Boolean,
    /** 是否使用了理想数值类型兼容。 */
    val usedIdealNumericCompatibility: Boolean,
    /** 是否依赖了 extend 参与。 */
    val usedExtendParticipation: Boolean,
) {
    /** 是否为泛型函数。 */
    val isGeneric: Boolean get() = typeParameters.isNotEmpty()

    companion object {
        /** 从候选构建 `FlatSignature`。 */
        fun create(candidate: Candidate): CfirFlatSignature {
            val symbol = candidate.symbol
            if (!symbol.isBound) {
                return CfirFlatSignature(candidate, emptyList(), emptyList(), 0, false, false, false)
            }

            val decl = symbol.cfir
            val typeParams: List<CfirTypeParameter>
            val paramTypes: List<ConeCangJieType?>

            fun parametersOf(parameters: List<CfirValueParameter>): List<ConeCangJieType?> {
                return parameters.map { vp ->
                    val declared = (vp.returnTypeRef as? CfirResolvedTypeRef)?.coneType
                    declared?.let { candidate.substitutor.substituteOrSelf(it) }
                }
            }

            when (decl) {
                is CfirFunction -> {
                    typeParams = decl.typeParameters
                    paramTypes = parametersOf(decl.valueParameters)
                }
                is CfirConstructor -> {
                    typeParams = decl.typeParameters
                    paramTypes = parametersOf(decl.valueParameters)
                }
                else -> {
                    typeParams = emptyList()
                    paramTypes = emptyList()
                }
            }

            return CfirFlatSignature(
                candidate,
                typeParams,
                paramTypes,
                candidate.numDefaults,
                candidate.usedQuestFallback,
                candidate.usedIdealNumericCompatibility,
                candidate.usedExtendParticipation,
            )
        }
    }
}
