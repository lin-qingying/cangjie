package org.cangnova.cangjie.analysis.api.cfir.utils

import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjFunctionLiteral
import org.cangnova.cangjie.psi.CjLambdaExpression


internal fun CjExpression.unwrap(): CjExpression {
    return when (this) {
//        is CjLabeledExpression -> baseExpression?.unwrap()
//        is CjAnnotatedExpression -> baseExpression?.unwrap()
        is CjFunctionLiteral -> (parent as? CjLambdaExpression)?.unwrap()
        else -> this
    } ?: this
}