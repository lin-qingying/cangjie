package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.SessionHolder
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirClassifierSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.ConeClassifierLookupTagWithFixedSymbol
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagImpl
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassifierLookupTag
import org.cangnova.cangjie.cfir.types.ConeClassLikeLookupTag
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeLookupTagBasedType
import org.cangnova.cangjie.name.ClassId

/**
 * @see toSymbol
 */

fun ConeClassifierLookupTag.toSymbol(useSiteSession: CfirSession): CfirClassifierSymbol<*>? {
    return when (this) {
        is ConeClassLikeLookupTag -> toSymbol(useSiteSession)
        is ConeClassifierLookupTagWithFixedSymbol -> this.symbol
        else -> error("missing branch for ${javaClass.name}")
    }
}

context(sessionHolder: SessionHolder)
fun ConeClassifierLookupTag.toSymbol(): CfirClassifierSymbol<*>? {
    return toSymbol(useSiteSession = sessionHolder.session)
}

fun ConeClassifierLookupTag.toClassLikeSymbol(useSiteSession: CfirSession): CfirClassLikeSymbol<*>? {
    return toSymbol(useSiteSession) as? CfirClassLikeSymbol<*>
}

context(sessionHolder: SessionHolder)
fun ConeClassifierLookupTag.toClassLikeSymbol(): CfirClassLikeSymbol<*>? {
    return toClassLikeSymbol(useSiteSession = sessionHolder.session)
}

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

fun ConeCangJieType.toSymbol(session: CfirSession): CfirClassifierSymbol<*>? {
    return (this as? ConeLookupTagBasedType)?.lookupTag?.toSymbol(session)
}

context(sessionHolder: SessionHolder)
fun ConeCangJieType.toSymbol(): CfirClassifierSymbol<*>? {
    return toSymbol(session = sessionHolder.session)
}

fun ConeCangJieType.toClassLikeSymbol(session: CfirSession): CfirClassLikeSymbol<*>? {
    return toSymbol(session) as? CfirClassLikeSymbol<*>
}

context(sessionHolder: SessionHolder)
fun ConeCangJieType.toClassLikeSymbol(): CfirClassLikeSymbol<*>? {
    return toClassLikeSymbol(session = sessionHolder.session)
}

fun ConeCangJieType.toTypeParameterSymbol(session: CfirSession): CfirTypeParameterSymbol? {
    return toSymbol(session) as? CfirTypeParameterSymbol
}

context(sessionHolder: SessionHolder)
fun ConeCangJieType.toTypeParameterSymbol(): CfirTypeParameterSymbol? {
    return toTypeParameterSymbol(session = sessionHolder.session)
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

fun ClassId.toSymbol(session: CfirSession): CfirClassLikeSymbol<*>? {
    return session.symbolProvider.getClassLikeSymbolByClassId(this)
}

context(sessionHolder: SessionHolder)
fun ClassId.toSymbol(): CfirClassLikeSymbol<*>? {
    return toSymbol(session = sessionHolder.session)
}
