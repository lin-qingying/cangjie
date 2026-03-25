package org.cangnova.cangjie.cfir.symbols

import org.cangnova.cangjie.cfir.types.ConeAttributes
import org.cangnova.cangjie.cfir.types.ConeLookupTagBasedType
import org.cangnova.cangjie.cfir.types.ConeTypeProjection
import org.cangnova.cangjie.name.ClassId


fun ClassId.toLookupTag(): ConeClassLikeLookupTagImpl {
    return ConeClassLikeLookupTagImpl(this)
}

fun CfirClassifierSymbol<*>.constructType(
    typeArguments: List<ConeTypeProjection> = emptyList(),
    attributes: ConeAttributes = ConeAttributes.Empty
): ConeLookupTagBasedType {
    return when (this) {
        is CfirTypeParameterSymbol -> ConeTypeParameterTypeImpl(this.toLookupTag(), attributes)
        is CfirClassifierSymbolWithClassId<*> -> constructType(typeArguments, attributes)

    }
}