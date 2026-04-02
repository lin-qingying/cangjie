package org.cangnova.cangjie.cfir.resolve.transformers.body.resolve

import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.resolvedType

internal inline var CfirExpression.resultType: ConeCangJieType
    get() = resolvedType
    set(type) {
        replaceConeTypeOrNull(type)
    }
