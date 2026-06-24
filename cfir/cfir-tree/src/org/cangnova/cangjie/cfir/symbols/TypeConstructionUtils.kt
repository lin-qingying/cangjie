package org.cangnova.cangjie.cfir.symbols

import org.cangnova.cangjie.cfir.types.ConeAttributes
import org.cangnova.cangjie.cfir.types.ConeClassLikeLookupTag
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeClassifierLookupTag
import org.cangnova.cangjie.cfir.types.ConeClassifierType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeLookupTagBasedType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTypeProjection
import org.cangnova.cangjie.name.ClassId


/**
 * 将 [ClassId] 转换为 class-like lookup tag。
 */
fun ClassId.toLookupTag(): ConeClassLikeLookupTagImpl {
    return ConeClassLikeLookupTagImpl(this)
}

/**
 * 基于 classifier lookup tag 构造类型。
 *
 * 类型参数 tag 构造 [ConeTypeParameterTypeImpl]，class-like tag 构造 class-like 类型。
 */
fun ConeClassifierLookupTag.constructType(
    typeArguments: List<ConeTypeProjection> = emptyList(),
    attributes: ConeAttributes = ConeAttributes.Empty,
): ConeLookupTagBasedType {
    return when (this) {
        is ConeTypeParameterLookupTag -> ConeTypeParameterTypeImpl(this, attributes)
        is ConeClassLikeLookupTag -> this.constructClassType(typeArguments, attributes)
        else -> error("! ${this::class}")
    }
}

/**
 * 基于 classifier 符号构造对应类型。
 */
fun CfirClassifierSymbol<*>.constructType(
    typeArguments: List<ConeTypeProjection> = emptyList(),
    attributes: ConeAttributes = ConeAttributes.Empty,
): ConeLookupTagBasedType {
    return when (this) {
        is CfirTypeParameterSymbol -> ConeTypeParameterTypeImpl(this.toLookupTag(), attributes)
        is CfirClassLikeSymbol<*> -> constructType(typeArguments, attributes)

    }
}

/**
 * 基于 class-like 符号构造对应的 class、interface、struct 或 enum 类型。
 */
fun CfirClassLikeSymbol<*>.constructType(
    typeArguments: List<ConeTypeProjection> = emptyList(),
    attributes: ConeAttributes = ConeAttributes.Empty,
): ConeLookupTagBasedType {

    return when (this) {
        is CfirInterfaceSymbol -> ConeClassLikeType(this.toLookupTag(), typeArguments, attributes, isInterface = true)
        is CfirStructSymbol -> ConeStructType(this.toLookupTag(), typeArguments, attributes)
        is CfirEnumSymbol -> ConeEnumType(this.toLookupTag(), typeArguments, attributes, isRefEnum)
        else -> ConeClassLikeType(this.toLookupTag(), typeArguments, attributes)
    }
}

/**
 * 构造类内部 `This` 视图类型。
 *
 * `This` 只属于 class 实例成员返回类型语义；普通 class 类型仍由 [constructType] 构造。
 */
fun CfirClassSymbol.constructThisType(
    typeArguments: List<ConeTypeProjection> = emptyList(),
    attributes: ConeAttributes = ConeAttributes.Empty,
): ConeClassLikeType {
    return ConeClassLikeType(this.toLookupTag(), typeArguments, attributes, isThisType = true)
}

/**
 * 基于 class-like lookup tag 构造普通 class-like 类型。
 */
fun ConeClassLikeLookupTag.constructClassType(
    typeArguments: List<ConeTypeProjection> = emptyList(),
    attributes: ConeAttributes = ConeAttributes.Empty,
): ConeClassifierType {
    return ConeClassLikeType(this, typeArguments, attributes)
}
