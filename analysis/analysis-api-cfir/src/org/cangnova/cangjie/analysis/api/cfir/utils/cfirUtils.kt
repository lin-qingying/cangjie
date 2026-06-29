package org.cangnova.cangjie.analysis.api.cfir.utils

import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjFunctionLiteral
import org.cangnova.cangjie.psi.CjLambdaExpression


/**
 * 解开表达式外层的函数 literal/lambda 包装，得到分析 API 应暴露的表达式节点。
 */
internal fun CjExpression.unwrap(): CjExpression {
    return when (this) {
//        is CjLabeledExpression -> baseExpression?.unwrap()
//        is CjAnnotatedExpression -> baseExpression?.unwrap()
        is CjFunctionLiteral -> (parent as? CjLambdaExpression)?.unwrap()
        else -> this
    } ?: this
}
