package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * 官方 IsFuncParameterTypesIdentical 的纯类型入口。
 *
 * 声明、继承更新和晚期实例化各自提供当前参数类型及明确代换，本层不发现 owner、
 * 不读取配对/角色，也不产生候选删除。错误类型不能形成有效的 IDENTICAL 签名。
 */
fun areFunctionParameterTypesIdentical(
    aParameterTypes: List<ConeCangJieType>,
    bParameterTypes: List<ConeCangJieType>,
    aSubstitutor: ConeSubstitutor,
    bSubstitutor: ConeSubstitutor,
    typeContext: ConeTypeContext,
): Boolean {
    if (aParameterTypes.size != bParameterTypes.size) return false
    return aParameterTypes.zip(bParameterTypes).all { (a, b) ->
        val first = aSubstitutor.substituteOrSelf(a)
        val second = bSubstitutor.substituteOrSelf(b)
        !first.containsErrorType() && !second.containsErrorType() &&
                AbstractTypeChecker.equalTypes(typeContext, first, second)
    }
}
