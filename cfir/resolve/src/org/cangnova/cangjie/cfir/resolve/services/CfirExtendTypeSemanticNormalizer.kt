package org.cangnova.cangjie.cfir.resolve.services

import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.types.CfirTypeSubstitutorByMap
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConePlaceholderType
import org.cangnova.cangjie.cfir.resolve.renderStableSemanticKey

internal class CfirExtendTypeSemanticNormalizer(
    extend: CfirExtend,
) {
    /**
     * extend 语义键不能只保留“第几个类型参数”，还要把约束信息编码进去。
     *
     * 否则 `extend<T where T <: A>` 与 `extend<T where T <: B>` 在接口形状相同的情况下
     * 会退化成同一个稳定键，导致复杂特化冲突被错误忽略。
     */
    private val substitutor = run {
        val baseSubstitutor = CfirTypeSubstitutorByMap(
            extend.typeParameters.mapIndexed { index, typeParameter ->
                typeParameter.name.asString() to ConePlaceholderType("__EXT_TP_$index")
            }.toMap(),
        )
        val boundFingerprints = extend.typeParameters.mapIndexed { index, typeParameter ->
            val fingerprint = typeParameter.bounds
                .mapNotNull { boundTypeRef ->
                    val boundType = (boundTypeRef as? CfirResolvedTypeRef)?.coneType ?: return@mapNotNull null
                    baseSubstitutor.substituteOrSelf(boundType).renderStableSemanticKey()
                }
                .sorted()
                .joinToString(separator = "&")
            val debugName = if (fingerprint.isEmpty()) {
                "__EXT_TP_$index"
            } else {
                "__EXT_TP_${index}_BOUNDS[$fingerprint]"
            }
            typeParameter.name.asString() to ConePlaceholderType(debugName)
        }.toMap()
        CfirTypeSubstitutorByMap(boundFingerprints)
    }

    fun semanticKeyOrNull(typeRef: CfirTypeRef): String? {
        val coneType = (typeRef as? CfirResolvedTypeRef)?.coneType ?: return null
        return canonicalize(coneType).renderStableSemanticKey()
    }

    private fun canonicalize(type: ConeCangJieType): ConeCangJieType {
        return substitutor.substituteOrSelf(type)
    }
}
