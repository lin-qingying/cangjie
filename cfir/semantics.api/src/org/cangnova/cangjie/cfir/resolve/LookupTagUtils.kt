package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagImpl
import org.cangnova.cangjie.utils.WeakPair
import org.cangnova.cangjie.cfir.types.ConeAttributes
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.withAttributes

/**
 * 将 class-like symbol 绑定到 lookup tag。
 *
 * 绑定使用弱引用 pair，避免 lookup tag 缓存强持有 session 或 symbol。
 */
fun ConeClassLikeLookupTagImpl.bindSymbolToLookupTag(session: CfirSession, symbol: CfirClassLikeSymbol<*>?) {
    boundSymbol = WeakPair(session, symbol)
}

/** 替换后的类型保留目标属性，并合并原使用点的属性；两种映射替换器共用同一合同。 */
fun ConeCangJieType.withCombinedAttributesFrom(other: ConeCangJieType): ConeCangJieType =
    withCombinedAttributesFrom(other.attributes)

private fun ConeCangJieType.withCombinedAttributesFrom(other: ConeAttributes): ConeCangJieType {
    if (other.isEmpty()) return this
    val combinedConeAttributes = attributes.add(other)
    return withAttributes(combinedConeAttributes)
}
