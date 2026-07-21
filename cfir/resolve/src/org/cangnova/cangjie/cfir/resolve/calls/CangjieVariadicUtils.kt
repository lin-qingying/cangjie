package org.cangnova.cangjie.cfir.resolve.calls

import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.arrayElementType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.name.OperatorNameConventions

/**
 * 仓颉普通变参调用语义由官方 `IsFuncDeclPossibleVariadic` 定义：
 * 最后一个非命名形参如果是 `Array<T>`，调用点可把若干位置实参收集为该数组。
 *
 * 这不同于 Kotlin 的显式 `vararg` 标记，因此 CFIR 不在参数声明上增加 Kotlin 式字段，
 * 而是在调用解析框架中按声明形状计算变参参数。
 */
internal fun Candidate.cangjieVariadicParameterForMapping(
    parameters: List<CfirValueParameter> = declaredParametersForMapping(),
): CfirValueParameter? {
    // synthetic common-invoke 的参数来自函数值类型，不是声明上的普通 Array 形参；
    // 函数值 arity 必须严格匹配，不能把最后一个 Array 参数重新解释成 variadic。
    if (callInfo.candidateForCommonInvokeReceiver != null) return null
    val declaration = symbol.takeIf { it.isBound }?.cfir as? CfirDeclaration
    return parameters.cangjieVariadicParameterOrNull(declaration)
}

/** 函数类型 receiver 的 synthetic `invoke` 使用严格函数 arity，不参与声明形状 variadic。 */
internal fun Candidate.isSyntheticFunctionTypeInvoke(): Boolean {
    val function = symbol.takeIf { it.isBound }?.cfir as? CfirNamedFunction ?: return false
    if (function.origin != org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin.Synthetic.FakeFunction) {
        return false
    }
    if (callInfo.name != OperatorNameConventions.INVOKE) return false
    return dispatchReceiverExpression()?.coneTypeOrNull?.fullyExpandedType(callInfo.session) is
            org.cangnova.cangjie.cfir.types.ConeFunctionType
}

/**
 * 一次普通变参调用可复用的声明形状。
 *
 * 参数映射、实参期望类型和调用完成必须共享同一份形状，避免各阶段分别推导
 * “最后一个非命名 Array 参数”后出现形参 owner 或固定参数数量不一致。
 */
internal data class CangjieVariadicCallShape(
    /** 被收集为数组的形参。 */
    val parameter: CfirValueParameter,
    /** 调用点逐元素检查时使用的期望类型。 */
    val elementType: ConeCangJieType,
    /** 变参形参之前的固定位置形参数量。 */
    val fixedPositionalArity: Int,
)

/**
 * 根据声明形状和调用点位置实参数量计算普通变参调用形状。
 *
 * 官方规则要求 `positionalArgs + 1 >= positionalParams`；因此允许省略整个变参数组，
 * 但不能用变参补齐更早缺失的固定位置参数。
 */
internal fun Candidate.cangjieVariadicCallShapeOrNull(
    parameters: List<CfirValueParameter>,
    positionalArgumentCount: Int,
): CangjieVariadicCallShape? {
    val parameter = cangjieVariadicParameterForMapping(parameters) ?: return null
    val parameterIndex = parameters.indexOf(parameter)
    if (parameterIndex < 0) return null

    val positionalParameterCount = parameters.indexOfLast { !it.isNamed } + 1
    if (positionalArgumentCount + 1 < positionalParameterCount) return null

    val elementType = parameter.cangjieVariadicElementTypeOrNull() ?: return null
    return CangjieVariadicCallShape(parameter, elementType, parameterIndex)
}

/** 从参数列表和声明形态中识别仓颉普通变参参数。 */
internal fun List<CfirValueParameter>.cangjieVariadicParameterOrNull(
    declaration: CfirDeclaration?,
): CfirValueParameter? {
    if (declaration is CfirEnumConstructor) return null
    if (declaration is CfirFunction && declaration.isNonVariadicOperator()) return null

    val positionalIndex = indexOfLast { !it.isNamed }
    if (positionalIndex < 0) return null

    val parameter = this[positionalIndex]
    return parameter.takeIf { it.cangjieVariadicElementTypeOrNull() != null }
}

/** 如果参数类型是 `Array<T>`，返回其元素类型 `T`。 */
internal fun CfirValueParameter.cangjieVariadicElementTypeOrNull(): ConeCangJieType? =
    returnTypeRef.coneTypeOrNull?.arrayElementType

/** 判断 operator 函数是否明确不允许按普通变参规则处理。 */
private fun CfirFunction.isNonVariadicOperator(): Boolean {
    if (!status.isOperator) return false
    if (this !is CfirNamedFunction) return false
    return name != OperatorNameConventions.INVOKE &&
        name != OperatorNameConventions.GET &&
        name != OperatorNameConventions.SET
}
