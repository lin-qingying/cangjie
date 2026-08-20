package org.cangnova.cangjie.cfir.resolve.calls.candidate

import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType
import org.cangnova.cangjie.cfir.types.contains
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.type.model.TypeCheckerProviderContext

/**
 * 在计算隐式返回类型前，按目标函数类型的参数部分筛除不可能的函数引用候选。
 *
 * 函数参数按逆变方向比较：目标函数可传入的值必须同样能传给候选函数。返回类型
 * 仍由 callable-reference candidate 的完整约束系统统一处理，避免在筛选阶段提前
 * 固化泛型推断。
 */
internal fun Candidate.hasCompatibleCallableReferenceParameterShape(
    expectedFunctionType: ConeFunctionType,
    typeContext: TypeCheckerProviderContext,
): Boolean {
    val function = symbol.takeIf { it.isBound }?.cfir as? CfirFunction ?: return false
    if (function.valueParameters.size != expectedFunctionType.parameterTypes.size) return false
    if (expectedFunctionType.parameterTypes.any { parameterType -> parameterType.contains { it is ConeTypeVariableType } }) {
        return true
    }
    return function.valueParameters.zip(expectedFunctionType.parameterTypes)
        .all { (parameter, expectedParameterType) ->
            val parameterType = (parameter.returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: return false
            val substitutedParameterType = substitutor.substituteOrSelf(parameterType)
            AbstractTypeChecker.isSubtypeOfForFunctionReference(
                typeContext,
                expectedParameterType,
                substitutedParameterType,
            )
        }
}
