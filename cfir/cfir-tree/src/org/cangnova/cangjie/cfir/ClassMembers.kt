package org.cangnova.cangjie.cfir

import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationDataRegistry
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.types.ConeClassLikeLookupTag
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeIntersectionType

fun CfirCallableSymbol<*>.containingClassLookupTag(): ConeClassLikeLookupTag? =
    cfir.containingClassLookupTag()
fun CfirCallableDeclaration.containingClassLookupTag(): ConeClassLikeLookupTag? =
    containingClassForStaticMemberAttr ?: dispatchReceiverClassLookupTagOrNull()
var CfirCallableDeclaration.containingClassForStaticMemberAttr: ConeClassLikeLookupTag? by CfirDeclarationDataRegistry.data(ContainingClassKey)
val CfirCallableDeclaration.isIntersectionOverride: Boolean get() = origin == CfirDeclarationOrigin.IntersectionOverride
var <D : CfirCallableDeclaration>


        D.originalForIntersectionOverrideAttr: D? by CfirDeclarationDataRegistry.data(IntersectionOverrideOriginalKey)
private object IntersectionOverrideOriginalKey : CfirDeclarationDataKey()

inline val <reified D : CfirCallableDeclaration> D.baseForIntersectionOverride: D?
    get() = if (isIntersectionOverride) originalForIntersectionOverrideAttr else null

inline val <reified S : CfirCallableSymbol<*>> S.baseForIntersectionOverride: S?
    get() = cfir.baseForIntersectionOverride?.symbol as S?

private object ContainingClassKey : CfirDeclarationDataKey()
fun CfirCallableSymbol<*>.dispatchReceiverClassLookupTagOrNull(): ConeClassLikeLookupTag? =
    cfir.dispatchReceiverClassLookupTagOrNull()
fun CfirCallableDeclaration.dispatchReceiverClassLookupTagOrNull(): ConeClassLikeLookupTag? =
    dispatchReceiverClassTypeOrNull()?.lookupTag
fun CfirCallableDeclaration.dispatchReceiverClassTypeOrNull(): ConeClassLikeType? =
    if (dispatchReceiverType is ConeIntersectionType && isIntersectionOverride)
        baseForIntersectionOverride!!.dispatchReceiverClassTypeOrNull()
    else
        dispatchReceiverType as? ConeClassLikeType
