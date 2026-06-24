package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagImpl
import org.cangnova.cangjie.utils.WeakPair

/**
 * 将 class-like symbol 绑定到 lookup tag。
 *
 * 绑定使用弱引用 pair，避免 lookup tag 缓存强持有 session 或 symbol。
 */
fun ConeClassLikeLookupTagImpl.bindSymbolToLookupTag(session: CfirSession, symbol: CfirClassLikeSymbol<*>?) {
    boundSymbol = WeakPair(session, symbol)
}
