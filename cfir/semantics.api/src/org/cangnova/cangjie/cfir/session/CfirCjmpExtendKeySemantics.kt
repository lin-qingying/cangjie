package org.cangnova.cangjie.cfir.session

import org.cangnova.cangjie.cfir.symbols.constructType

import org.cangnova.cangjie.cfir.resolve.substitution.substitutorByMap

import org.cangnova.cangjie.cfir.declarations.CfirCjmpExtendKey
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.type.AbstractTypeChecker

/** 无效类型明确没有 merge key；目标、接口和每个泛型参数的约束均保留真实 Cone 类型。 */
fun CfirExtend.cjmpOriginalExtendKey(): CfirCjmpExtendKey? {
    val target = extendedTypeRef.coneTypeOrNull?.takeUnless { it.containsErrorType() } ?: return null
    val interfaces = superTypeRefs.map { it.coneTypeOrNull?.takeUnless { type -> type.containsErrorType() } ?: return null }
    val bounds = typeParameters.map { parameter ->
        parameter.bounds.map { it.coneTypeOrNull?.takeUnless { type -> type.containsErrorType() } ?: return null }
    }
    return CfirCjmpExtendKey(target, typeParameters.map { it.symbol }, interfaces, bounds)
}

/** 官方 extend 键的类型等价关系；只用于缓存外的准备，不能用打印文本或 ClassId 代替。 */
fun CfirCjmpExtendKey.sameKeyAs(other: CfirCjmpExtendKey, session: CfirSession): Boolean {
    if (typeParameters.size != other.typeParameters.size) return false
    val mapping = other.typeParameters.zip(typeParameters).associate { (common, specific) -> common.cfir to specific.cfir }
    val substitutor = substitutorByMap((mapping).entries.associate { (origin, target) -> origin.symbol to target.symbol.constructType() }, session)
    fun equal(first: ConeCangJieType, second: ConeCangJieType): Boolean =
        AbstractTypeChecker.equalTypes(session.typeContext, first, substitutor.substituteOrSelf(second))
    fun sameSet(first: List<ConeCangJieType>, second: List<ConeCangJieType>): Boolean {
        if (first.size != second.size) return false
        val unmatched = second.toMutableList()
        for (type in first) {
            val index = unmatched.indexOfFirst { equal(type, it) }
            if (index < 0) return false
            unmatched.removeAt(index)
        }
        return true
    }
    return equal(target, other.target) && sameSet(interfaces, other.interfaces) &&
            bounds.indices.all { sameSet(bounds[it], other.bounds[it]) }
}
