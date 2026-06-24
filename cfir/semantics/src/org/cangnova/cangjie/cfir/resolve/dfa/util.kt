package org.cangnova.cangjie.cfir.resolve.dfa

import org.cangnova.cangjie.cfir.types.ConeInferenceContext
import org.cangnova.cangjie.cfir.types.ConeCangJieType

/**
 * 根据类型陈述计算 smart cast 后的类型。
 *
 * 当存在上界类型时，将所有上界与变量原始类型求交；没有上界时保留原始类型。
 */
fun TypeStatement.smartCastedType(context: ConeInferenceContext): ConeCangJieType =
    if (upperTypes.isNotEmpty()) {
        context.intersectTypes(upperTypes.toMutableList().also { it += variable.originalType }) as ConeCangJieType
    } else {
        variable.originalType
    }
