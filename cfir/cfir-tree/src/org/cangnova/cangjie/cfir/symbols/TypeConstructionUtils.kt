package org.cangnova.cangjie.cfir.symbols

import org.cangnova.cangjie.cfir.types.ConeAttributes
import org.cangnova.cangjie.cfir.types.ConeClassLikeLookupTag
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeClassifierLookupTag
import org.cangnova.cangjie.cfir.types.ConeClassifierType
import org.cangnova.cangjie.cfir.types.ConeLookupTagBasedType
import org.cangnova.cangjie.cfir.types.ConeTypeProjection
import org.cangnova.cangjie.name.ClassId


fun ClassId.toLookupTag(): ConeClassLikeLookupTagImpl {
    return ConeClassLikeLookupTagImpl(this)
}
fun ConeClassifierLookupTag.constructType(
    typeArguments: List<  ConeTypeProjection> = emptyList(),

    attributes: ConeAttributes = ConeAttributes.Empty
): ConeLookupTagBasedType {
    return when (this) {
        is ConeTypeParameterLookupTag -> ConeTypeParameterTypeImpl(this, attributes)
        is ConeClassLikeLookupTag -> this.constructClassType(typeArguments, attributes)
        else -> error("! ${this::class}")
    }
}
fun CfirClassifierSymbol<*>.constructType(
    typeArguments: List<ConeTypeProjection> = emptyList(),
    attributes: ConeAttributes = ConeAttributes.Empty
): ConeLookupTagBasedType {
    return when (this) {
        is CfirTypeParameterSymbol -> ConeTypeParameterTypeImpl(this.toLookupTag(), attributes)
        is CfirClassLikeSymbol<*> -> constructType(typeArguments, attributes)

    }
}
fun CfirClassLikeSymbol<*>.constructType(
    typeArguments: List<ConeTypeProjection> = emptyList(),
    attributes: ConeAttributes = ConeAttributes.Empty
): ConeLookupTagBasedType {

    return ConeClassLikeType(this.toLookupTag(), typeArguments, attributes)
}

/**
 * 构造类内部 `This` 视图类型。
 *
 * `This` 只属于 class 实例成员返回类型语义；普通 class 类型仍由 [constructType] 构造。
 */
fun CfirClassSymbol.constructThisType(
    typeArguments: List<ConeTypeProjection> = emptyList(),
    attributes: ConeAttributes = ConeAttributes.Empty
): ConeClassLikeType {
    return ConeClassLikeType(this.toLookupTag(), typeArguments, attributes, isThisType = true)
}
fun ConeClassLikeLookupTag.constructClassType(
    typeArguments: List<  ConeTypeProjection> = emptyList(),

    attributes: ConeAttributes = ConeAttributes.Empty
): ConeClassifierType {
    return ConeClassLikeType(this, typeArguments, attributes)
}
