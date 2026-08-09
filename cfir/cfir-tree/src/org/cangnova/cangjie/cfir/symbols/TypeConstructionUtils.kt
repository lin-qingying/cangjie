package org.cangnova.cangjie.cfir.symbols

import org.cangnova.cangjie.cfir.types.ConeAttributes
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeLookupTag
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeClassifierLookupTag
import org.cangnova.cangjie.cfir.types.ConeClassifierType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeLookupTagBasedType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
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
 * 基于类型参数符号构造其语义类型，并保留窄返回类型供类型参数专用调用链使用。
 */
fun CfirTypeParameterSymbol.constructType(
    attributes: ConeAttributes = ConeAttributes.Empty,
): ConeTypeParameterTypeImpl = ConeTypeParameterTypeImpl(toLookupTag(), attributes)

/**
 * 基于 classifier 符号构造对应类型。
 */
fun CfirClassifierSymbol<*>.constructType(
    typeArguments: List<ConeTypeProjection> = emptyList(),
    attributes: ConeAttributes = ConeAttributes.Empty,
): ConeCangJieType {
    return when (this) {
        is CfirTypeParameterSymbol -> constructType(attributes)
        is CfirClassLikeSymbol<*> -> constructType(typeArguments, attributes)

    }
}

/**
 * 基于 class-like 符号构造对应的语义类型。
 *
 * Primitive 声明虽然以合成 class-like symbol 参与名称与成员查询，其语义类型仍必须是
 * [ConePrimitiveType]；不能泄漏为同 ClassId 的 [ConeClassLikeType]，否则 extend、父类型和
 * subtype 查询会进入错误的 class-like 索引路径。
 */
fun CfirClassLikeSymbol<*>.constructType(
    typeArguments: List<ConeTypeProjection> = emptyList(),
    attributes: ConeAttributes = ConeAttributes.Empty,
): ConeCangJieType {

    return when (this) {
        is CfirPrimitiveTypeSymbol -> {
            require(typeArguments.isEmpty()) { "Primitive type $name cannot have type arguments" }
            ConePrimitiveType(kind, attributes)
        }
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
