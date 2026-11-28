package org.cangnova.cangjie.cfir.resolve.substitution

import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.resolve.withCombinedAttributesFrom
import org.cangnova.cangjie.type.model.TypeConstructorMarker

/**
 * 根据类型构造器映射创建替换器。
 */
fun createTypeSubstitutorByTypeConstructor(
    map: Map<TypeConstructorMarker, ConeCangJieType>,
    context: ConeTypeContext,
    approximateIntegerLiterals: Boolean,
): ConeSubstitutor {
    if (map.isEmpty()) return ConeSubstitutor.Empty
    return ConeTypeSubstitutorByTypeConstructor(map, context, approximateIntegerLiterals)
}

/**
 * 以类型构造器身份为 key 的 concrete cone 替换器。
 */
private class ConeTypeSubstitutorByTypeConstructor(
    /**
     * 类型构造器到替换目标类型的映射。
     */
    private val map: Map<TypeConstructorMarker, ConeCangJieType>,
    typeContext: ConeTypeContext,
    /**
     * 是否在替换结果中把 integer literal 类型近似为具体 primitive。
     */
    private val approximateIntegerLiterals: Boolean,
) : AbstractConeSubstitutor(typeContext) {

    /**
     * 根据 [type] 的可替换构造器查表得到替换类型。
     */
    override fun substituteType(type: ConeCangJieType): ConeCangJieType? {
        if (type !is ConeLookupTagBasedType && type !is ConeStubType && type !is ConeTypeVariableType) return null
        val constructor = with(typeContext) { type.typeConstructor() }
        val newType = map[constructor] ?: return null
        val approximatedIntegerLiteralType = if (approximateIntegerLiterals) newType.approximateIntegerLiteralType() else newType
        return approximatedIntegerLiteralType.withCombinedAttributesFrom(type)
    }

    /**
     * 返回替换映射的调试文本。
     */
    override fun toString(): String {
        return map.entries.joinToString(prefix = "{", postfix = "}", separator = " | ") { (constructor, type) ->
            "$constructor -> $type"
        }
    }
}

/**
 * 必要时把 ideal/integer literal 类型近似为具体 primitive 类型。
 */
private fun ConeCangJieType.approximateIntegerLiteralType(): ConeCangJieType {
    return when (this) {
        is ConeIdealLiteralType -> getApproximatedType()
        is ConePrimitiveType -> IdealTypeResolver.resolveIfIdeal(this)
        else -> this
    }
}
