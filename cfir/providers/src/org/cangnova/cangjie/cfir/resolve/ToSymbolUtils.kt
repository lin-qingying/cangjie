package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.SessionHolder
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagImpl
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeLookupTag
import org.cangnova.cangjie.cfir.types.ConeClassLikeType

/**
 * @see toSymbol
 */

fun ConeClassLikeLookupTag.toSymbol(useSiteSession: CfirSession): CfirClassLikeSymbol<*>? {

    (this as? ConeClassLikeLookupTagImpl)?.boundSymbol?.takeIf { it.first === useSiteSession }?.let { return it.second }

    return useSiteSession.symbolProvider.getClassLikeSymbolByClassId(classId).also {
        (this as? ConeClassLikeLookupTagImpl)?.bindSymbolToLookupTag(useSiteSession, it)
    }
}

context(sessionHolder: SessionHolder)
fun ConeClassLikeLookupTag.toSymbol(): CfirClassLikeSymbol<*>? {
    return toSymbol(useSiteSession = sessionHolder.session)
}

fun ConeClassLikeLookupTag.toClassSymbol(session: CfirSession): CfirClassSymbol? {
    return toSymbol(session) as? CfirClassSymbol
}

context(sessionHolder: SessionHolder)
fun ConeClassLikeLookupTag.toClassSymbol(): CfirClassSymbol? {
    return toClassSymbol(session = sessionHolder.session)
}

fun ConeClassLikeType.toSymbol(session: CfirSession): CfirClassLikeSymbol<*>? {
    return lookupTag.toSymbol(session)
}

context(sessionHolder: SessionHolder)
fun ConeClassLikeType.toSymbol(): CfirClassLikeSymbol<*>? {
    return toSymbol(session = sessionHolder.session)
}

fun ConeCangJieType.toClassSymbol(session: CfirSession): CfirClassSymbol? {
    return (this as? ConeClassLikeType)?.toClassSymbol(session)
}

context(sessionHolder: SessionHolder)
fun ConeCangJieType.toClassSymbol(): CfirClassSymbol? {
    return toClassSymbol(session = sessionHolder.session)
}

fun ConeClassLikeType.toClassSymbol(session: CfirSession): CfirClassSymbol? {
    return (fullyExpandedType(session) as? ConeClassLikeType)?.toSymbol(session) as? CfirClassSymbol
}

context(sessionHolder: SessionHolder)
fun ConeClassLikeType.toClassSymbol(): CfirClassSymbol? {
    return toClassSymbol(session = sessionHolder.session)
}
