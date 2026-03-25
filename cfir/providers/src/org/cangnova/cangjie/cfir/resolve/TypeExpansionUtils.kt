package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.SessionHolder
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.util.expandedConeType
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType

context(sessionHolder: SessionHolder)
fun ConeCangJieType.fullyExpandedType(): ConeCangJieType {
    return fullyExpandedType(sessionHolder.session, expandedConeType = CfirTypeAlias::expandedConeTypeWithEnsuredPhase)
}
fun CfirTypeAlias.expandedConeTypeWithEnsuredPhase(): ConeClassLikeType? {
    lazyResolveToPhase(CfirResolvePhase.SUPER_TYPES)
    return expandedConeType
}
/**
 * @see fullyExpandedType (the first function in the file)
 * @return the expanded type or the same instance if top-level constructor is not expandable type alias
 */
fun ConeCangJieType.fullyExpandedType(
    useSiteSession: CfirSession,
    expandedConeType: (CfirTypeAlias) -> ConeClassLikeType? = CfirTypeAlias::expandedConeTypeWithEnsuredPhase,
): ConeCangJieType= when (this) {

    is ConeClassLikeType -> fullyExpandedType(useSiteSession, expandedConeType)
    else -> this
}
