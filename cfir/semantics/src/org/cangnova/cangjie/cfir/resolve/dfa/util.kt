package org.cangnova.cangjie.cfir.resolve.dfa

import org.cangnova.cangjie.cfir.types.ConeInferenceContext
import org.cangnova.cangjie.cfir.types.ConeCangJieType

fun TypeStatement.smartCastedType(context: ConeInferenceContext): ConeCangJieType =
    if (upperTypes.isNotEmpty()) {
        context.intersectTypes(upperTypes.toMutableList().also { it += variable.originalType }) as ConeCangJieType
    } else {
        variable.originalType
    }
