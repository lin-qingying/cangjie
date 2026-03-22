package org.cangnova.cangjie.cfir.constraints

import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeTypeParameterLookupTag

data class CfirTypeVariable(
    val typeParameter: CfirTypeParameterSymbol,
    val freshTypeId: Int,
    val lookupTag: ConeTypeParameterLookupTag,
    val upperBounds: MutableList<ConeCangJieType> = mutableListOf(),
    val lowerBounds: MutableList<ConeCangJieType> = mutableListOf(),
    /** 必须从中选一的候选类型集合（对标官方 TyVarBounds.sum） */
    val sumBounds: MutableList<ConeCangJieType> = mutableListOf(),
) {
    /** 贪心等式解（对标官方 TyVarBounds.eq） */
    var equalitySolution: ConeCangJieType? = null

    var fixedType: ConeCangJieType? = null
}
