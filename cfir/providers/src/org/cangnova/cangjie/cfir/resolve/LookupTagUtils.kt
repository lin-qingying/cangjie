package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagImpl
import org.cangnova.cangjie.utils.WeakPair

fun ConeClassLikeLookupTagImpl.bindSymbolToLookupTag(session: CfirSession, symbol: CfirClassLikeSymbol<*>?) {
    boundSymbol = WeakPair(session, symbol)
}
