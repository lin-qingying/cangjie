package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.resolve.substitution.createTypeSubstitutorByTypeConstructor
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.type.model.TypeConstructorMarker

/**
 * 对位官方 GenerateTypeMappingByTy / GenerateTypeMappingByArgs 的部分映射。
 * 只映射源类型中真实泛型节点；不相容的非泛型分支不贡献映射，重复节点保留首次写入。
 * 该操作不检查 where 约束，也不把部分映射当作 extend 适用性成功。
 */
fun generateGenericTypeMapping(
    sourceType: ConeCangJieType,
    instantiatedType: ConeCangJieType,
    typeContext: ConeTypeContext,
    normalize: (ConeCangJieType) -> ConeCangJieType,
): ConeSubstitutor {
    val replacements = linkedMapOf<TypeConstructorMarker, ConeCangJieType>()
    fun collect(source: ConeCangJieType, target: ConeCangJieType) {
        val generic = normalize(source)
        val actual = normalize(target)
        if (generic is ConeTypeParameterType) {
            replacements.putIfAbsent(generic.lookupTag, actual)
            return
        }
        if (!generic.hasSameGenericMappingKind(actual) || generic.classIdOrPrimitiveClassId != actual.classIdOrPrimitiveClassId) return
        val genericArguments = generic.nativeGenericMappingArguments()
        val actualArguments = actual.nativeGenericMappingArguments()
        if (genericArguments.size != actualArguments.size) return
        genericArguments.zip(actualArguments).forEach { (parameter, argument) -> collect(parameter, argument) }
    }
    collect(sourceType, instantiatedType)
    return createTypeSubstitutorByTypeConstructor(replacements, typeContext, false)
}

/**
 * Cone 的具体节点类型表示 native TyKind；primitive 与 class/interface 共用载体时比较其显式语义字段。
 * 本比较不含类型实参、函数 ABI 或 VArray 长度，它们不是 GenerateTypeMappingByTy 的 kind 条件。
 */
private fun ConeCangJieType.hasSameGenericMappingKind(other: ConeCangJieType): Boolean {
    if (this::class != other::class) return false
    return when (this) {
        is ConePrimitiveType -> kind == (other as ConePrimitiveType).kind
        is ConeClassLikeType -> {
            other as ConeClassLikeType
            // ClassThisTy 调用 ClassTy 构造器，native kind 仍为 CLASS；This 标志不属于映射入口的 kind 条件。
            isInterface == other.isInterface
        }
        else -> true
    }
}

private fun ConeCangJieType.nativeGenericMappingArguments(): List<ConeCangJieType> = when (this) {
    is ConeFunctionType -> parameterTypes + returnType
    is ConeTupleType -> elementTypes
    is ConePointerType -> listOf(pointeeType)
    is ConeVArrayType -> listOf(elementType)
    else -> typeArguments.map { it.type }
}
