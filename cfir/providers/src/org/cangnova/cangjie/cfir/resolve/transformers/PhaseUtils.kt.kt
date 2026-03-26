package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.toSymbol
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType

fun ConeCangJieType?.ensureResolvedTypeDeclaration(
    useSiteSession: CfirSession,
    requiredPhase: CfirResolvePhase = CfirResolvePhase.DECLARATIONS,
) {
    if (this !is ConeClassLikeType) return

    lookupTag.toSymbol(useSiteSession)?.lazyResolveToPhase(requiredPhase)
    fullyExpandedType(useSiteSession).lookupTag.toSymbol(useSiteSession)?.lazyResolveToPhase(requiredPhase)
}