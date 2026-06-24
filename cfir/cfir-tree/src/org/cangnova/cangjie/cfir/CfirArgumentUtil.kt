package org.cangnova.cangjie.cfir

import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.expressions.CfirArgumentList
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirResolvedArgumentList
import org.cangnova.cangjie.cfir.expressions.CfirResolvedArgumentListForErrorCall
import org.cangnova.cangjie.cfir.expressions.CfirResolvedArgumentListImpl

/**
 * 为错误调用构造保留原始实参列表的 resolved argument list。
 *
 * @param original 原始实参列表。
 * @param mapping 实参表达式到形参的映射；错误调用允许映射到 `null`。
 */
fun buildArgumentListForErrorCall(
    original: CfirArgumentList,
    mapping: LinkedHashMap<CfirExpression, CfirValueParameter?>,
): CfirArgumentList {
    return CfirResolvedArgumentListForErrorCall(original, mapping)
}

/**
 * 构造正常调用解析后的实参列表。
 *
 * @param original 原始实参列表；合成调用可以没有原始列表。
 * @param mapping 实参表达式到已解析形参的映射。
 */
fun buildResolvedArgumentList(
    original: CfirArgumentList?,
    mapping: LinkedHashMap<CfirExpression, CfirValueParameter>,
): CfirResolvedArgumentList {
    return CfirResolvedArgumentListImpl(original, mapping)
}
