package org.cangnova.cangjie.cfir.resolve.services

import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.resolve.calls.CfirTypeSubstitutorByMap
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.types.ConeTypeParameterLookupTag
import org.cangnova.cangjie.cfir.types.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.renderSemanticKey

internal class CfirExtendTypeSemanticNormalizer(
    extend: CfirExtend,
) {
    private val substitutor = CfirTypeSubstitutorByMap(
        extend.typeParameters.mapIndexed { index, typeParameter ->
            typeParameter.name.asString() to ConeTypeParameterType(ConeTypeParameterLookupTag("__EXT_TP_$index"))
        }.toMap(),
    )

    fun semanticKeyOrNull(typeRef: CfirTypeRef): String? {
        val coneType = (typeRef as? CfirResolvedTypeRef)?.coneType ?: return null
        return canonicalize(coneType).renderSemanticKey()
    }

    private fun canonicalize(type: ConeCangjieType): ConeCangjieType {
        return substitutor.substituteOrSelf(type)
    }
}

