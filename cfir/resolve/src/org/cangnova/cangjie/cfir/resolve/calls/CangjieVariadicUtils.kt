package org.cangnova.cangjie.cfir.resolve.calls

import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
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
    val declaration = symbol.takeIf { it.isBound }?.cfir as? CfirDeclaration
    return parameters.cangjieVariadicParameterOrNull(declaration)
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
