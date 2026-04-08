package org.cangnova.cangjie.cfir

import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.expressions.CfirArgumentList
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirResolvedArgumentList
import org.cangnova.cangjie.cfir.expressions.CfirResolvedArgumentListForErrorCall
import org.cangnova.cangjie.cfir.expressions.CfirResolvedArgumentListImpl


fun buildArgumentListForErrorCall(
    original: CfirArgumentList,
    mapping: LinkedHashMap<CfirExpression, CfirValueParameter?>,
): CfirArgumentList {
    return CfirResolvedArgumentListForErrorCall(original, mapping)
}

fun buildResolvedArgumentList(
    original: CfirArgumentList?,
    mapping: LinkedHashMap<CfirExpression, CfirValueParameter>,
): CfirResolvedArgumentList {
    return CfirResolvedArgumentListImpl(original, mapping)
}