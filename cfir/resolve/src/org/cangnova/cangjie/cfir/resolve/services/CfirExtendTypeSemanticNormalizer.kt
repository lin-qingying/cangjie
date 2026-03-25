package org.cangnova.cangjie.cfir.resolve.services

import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.constraints.CfirTypeSubstitutorByMap
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConePlaceholderType
import org.cangnova.cangjie.cfir.resolve.renderStableSemanticKey

internal class CfirExtendTypeSemanticNormalizer(
    extend: CfirExtend,
) {
    private val substitutor = CfirTypeSubstitutorByMap(
        extend.typeParameters.mapIndexed { index, typeParameter ->
            typeParameter.name.asString() to ConePlaceholderType("__EXT_TP_$index")
        }.toMap(),
    )

    fun semanticKeyOrNull(typeRef: CfirTypeRef): String? {
        val coneType = (typeRef as? CfirResolvedTypeRef)?.coneType ?: return null
        return canonicalize(coneType).renderStableSemanticKey()
    }

    private fun canonicalize(type: ConeCangJieType): ConeCangJieType {
        return substitutor.substituteOrSelf(type)
    }
}
