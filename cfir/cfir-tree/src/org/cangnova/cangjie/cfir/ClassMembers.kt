package org.cangnova.cangjie.cfir

import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationDataRegistry
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
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
val CfirCallableDeclaration.isSubstitutionOrIntersectionOverride: Boolean
    get() = isSubstitutionOverride || isIntersectionOverride
val CfirCallableDeclaration.isCopyCreatedInScope: Boolean
    get() = isSubstitutionOrIntersectionOverride
val CfirCallableDeclaration.canHaveDeferredReturnTypeCalculation: Boolean
    get() = isCopyCreatedInScope
var <D : CfirCallableDeclaration>


        D.originalForIntersectionOverrideAttr: D? by CfirDeclarationDataRegistry.data(IntersectionOverrideOriginalKey)
private object IntersectionOverrideOriginalKey : CfirDeclarationDataKey()

inline val <reified D : CfirCallableDeclaration> D.baseForIntersectionOverride: D?
    get() = if (isIntersectionOverride) originalForIntersectionOverrideAttr else null

inline val <reified S : CfirCallableSymbol<*>> S.baseForIntersectionOverride: S?
    get() = cfir.baseForIntersectionOverride?.symbol as S?

val CfirCallableDeclaration.isSubstitutionOverride: Boolean
    get() = origin is CfirDeclarationOrigin.SubstitutionOverride

private object SubstitutedOverrideOriginalKey : CfirDeclarationDataKey()

var <D : CfirCallableDeclaration>
        D.originalForSubstitutionOverrideAttr: D? by CfirDeclarationDataRegistry.data(SubstitutedOverrideOriginalKey)

inline val <reified D : CfirCallableDeclaration> D.originalForSubstitutionOverride: D?
    get() = if (isSubstitutionOverride) {
        originalForSubstitutionOverrideAttr
    } else {
        null
    }

inline val <reified S : CfirCallableSymbol<*>> S.originalForSubstitutionOverride: S?
    get() = cfir.originalForSubstitutionOverride?.symbol as S?

inline fun <reified D : CfirCallableDeclaration> D.originalIfFakeOverride(): D? =
    originalForSubstitutionOverride ?: baseForIntersectionOverride

inline fun <reified D : CfirCallableDeclaration> D.originalIfFakeOverrideOrDelegated(): D? =
    originalIfFakeOverride()

inline fun <reified S : CfirCallableSymbol<*>> S.originalIfFakeOverride(): S? =
    cfir.originalIfFakeOverride()?.symbol as S?

inline fun <reified D : CfirCallableDeclaration> D.unwrapFakeOverridesOrDelegated(): D {
    var current = this
    do {
        val next = current.originalIfFakeOverrideOrDelegated() ?: return current
        current = next
    } while (true)
}

inline fun <reified D : CfirCallableSymbol<*>> D.unwrapFakeOverridesOrDelegated(): D =
    cfir.unwrapFakeOverridesOrDelegated().symbol as D

inline fun <reified D : CfirCallableDeclaration> D.unwrapSubstitutionOverrides(): D {
    var current = this
    do {
        val next = current.originalForSubstitutionOverride ?: return current
        current = next
    } while (true)
}

inline fun <reified S : CfirCallableSymbol<*>> S.unwrapSubstitutionOverrides(): S =
    cfir.unwrapSubstitutionOverrides().symbol as S

private object CorrespondingPropertyKey : CfirDeclarationDataKey()

/**
 * 主构造参数提升为属性时，记录它在声明树中的对应属性。
 */
var CfirValueParameter.correspondingProperty: CfirProperty? by CfirDeclarationDataRegistry.data(CorrespondingPropertyKey)

private object IsCatchParameterPropertyKey : CfirDeclarationDataKey()

/**
 * catch 参数在 CFIR 中按 local property 建模，但语义上不是普通属性声明。
 * 用这个标记把它和常规 `prop` 区分开，供 checker / renderer / CFA 精确识别。
 */
var CfirProperty.isCatchParameter: Boolean? by CfirDeclarationDataRegistry.data(IsCatchParameterPropertyKey)

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
