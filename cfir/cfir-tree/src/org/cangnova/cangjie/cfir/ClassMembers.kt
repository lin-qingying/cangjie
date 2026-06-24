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

/**
 * 从 callable symbol 读取其所在 class 的 lookup tag。
 */
fun CfirCallableSymbol<*>.containingClassLookupTag(): ConeClassLikeLookupTag? =
    cfir.containingClassLookupTag()

/**
 * 从 callable 声明读取其所在 class 的 lookup tag。
 */
fun CfirCallableDeclaration.containingClassLookupTag(): ConeClassLikeLookupTag? =
    containingClassForStaticMemberAttr ?: dispatchReceiverClassLookupTagOrNull()

/**
 * 静态成员声明显式记录的所属 class lookup tag。
 */
var CfirCallableDeclaration.containingClassForStaticMemberAttr: ConeClassLikeLookupTag? by CfirDeclarationDataRegistry.data(ContainingClassKey)

/**
 * 当前 callable 是否是交叉类型合成出来的 override。
 */
val CfirCallableDeclaration.isIntersectionOverride: Boolean get() = origin == CfirDeclarationOrigin.IntersectionOverride

/**
 * 当前 callable 是否是 substitution override 或 intersection override。
 */
val CfirCallableDeclaration.isSubstitutionOrIntersectionOverride: Boolean
    get() = isSubstitutionOverride || isIntersectionOverride

/**
 * 当前 callable 是否是在 scope 层复制出的声明。
 */
val CfirCallableDeclaration.isCopyCreatedInScope: Boolean
    get() = isSubstitutionOrIntersectionOverride

/**
 * 当前 callable 是否允许延迟计算返回类型。
 */
val CfirCallableDeclaration.canHaveDeferredReturnTypeCalculation: Boolean
    get() = isCopyCreatedInScope

/**
 * intersection override 对应的原始 callable。
 */
var <D : CfirCallableDeclaration>


        D.originalForIntersectionOverrideAttr: D? by CfirDeclarationDataRegistry.data(IntersectionOverrideOriginalKey)

/**
 * intersection override 原始声明的附加数据键。
 */
private object IntersectionOverrideOriginalKey : CfirDeclarationDataKey()

/**
 * 当前声明是 intersection override 时返回其原始声明。
 */
inline val <reified D : CfirCallableDeclaration> D.baseForIntersectionOverride: D?
    get() = if (isIntersectionOverride) originalForIntersectionOverrideAttr else null

/**
 * 当前 symbol 是 intersection override 时返回其原始 symbol。
 */
inline val <reified S : CfirCallableSymbol<*>> S.baseForIntersectionOverride: S?
    get() = cfir.baseForIntersectionOverride?.symbol as S?

/**
 * 当前 callable 是否是 substitution override。
 */
val CfirCallableDeclaration.isSubstitutionOverride: Boolean
    get() = origin is CfirDeclarationOrigin.SubstitutionOverride

/**
 * substitution override 原始声明的附加数据键。
 */
private object SubstitutedOverrideOriginalKey : CfirDeclarationDataKey()

/**
 * substitution override 对应的原始 callable。
 */
var <D : CfirCallableDeclaration>
        D.originalForSubstitutionOverrideAttr: D? by CfirDeclarationDataRegistry.data(SubstitutedOverrideOriginalKey)

/**
 * 当前声明是 substitution override 时返回其原始声明。
 */
inline val <reified D : CfirCallableDeclaration> D.originalForSubstitutionOverride: D?
    get() = if (isSubstitutionOverride) {
        originalForSubstitutionOverrideAttr
    } else {
        null
    }

/**
 * 当前 symbol 是 substitution override 时返回其原始 symbol。
 */
inline val <reified S : CfirCallableSymbol<*>> S.originalForSubstitutionOverride: S?
    get() = cfir.originalForSubstitutionOverride?.symbol as S?

/**
 * 当前声明是 fake override 时返回原始声明。
 */
inline fun <reified D : CfirCallableDeclaration> D.originalIfFakeOverride(): D? =
    originalForSubstitutionOverride ?: baseForIntersectionOverride

/**
 * 当前声明是 fake override 或 delegated override 时返回原始声明。
 */
inline fun <reified D : CfirCallableDeclaration> D.originalIfFakeOverrideOrDelegated(): D? =
    originalIfFakeOverride()

/**
 * 当前 symbol 是 fake override 时返回原始 symbol。
 */
inline fun <reified S : CfirCallableSymbol<*>> S.originalIfFakeOverride(): S? =
    cfir.originalIfFakeOverride()?.symbol as S?

/**
 * 递归剥离 fake override / delegated override，返回最初声明。
 */
inline fun <reified D : CfirCallableDeclaration> D.unwrapFakeOverridesOrDelegated(): D {
    var current = this
    do {
        val next = current.originalIfFakeOverrideOrDelegated() ?: return current
        current = next
    } while (true)
}

/**
 * 递归剥离 symbol 对应声明的 fake override / delegated override。
 */
inline fun <reified D : CfirCallableSymbol<*>> D.unwrapFakeOverridesOrDelegated(): D =
    cfir.unwrapFakeOverridesOrDelegated().symbol as D

/**
 * 递归剥离 substitution override，返回最初声明。
 */
inline fun <reified D : CfirCallableDeclaration> D.unwrapSubstitutionOverrides(): D {
    var current = this
    do {
        val next = current.originalForSubstitutionOverride ?: return current
        current = next
    } while (true)
}

/**
 * 递归剥离 symbol 对应声明的 substitution override。
 */
inline fun <reified S : CfirCallableSymbol<*>> S.unwrapSubstitutionOverrides(): S =
    cfir.unwrapSubstitutionOverrides().symbol as S

/**
 * 主构造参数对应属性的附加数据键。
 */
private object CorrespondingPropertyKey : CfirDeclarationDataKey()

/**
 * 主构造参数提升为属性时，记录它在声明树中的对应属性。
 */
var CfirValueParameter.correspondingProperty: CfirProperty? by CfirDeclarationDataRegistry.data(CorrespondingPropertyKey)

/**
 * catch 参数属性标记的附加数据键。
 */
private object IsCatchParameterPropertyKey : CfirDeclarationDataKey()

/**
 * catch 参数在 CFIR 中按 local property 建模，但语义上不是普通属性声明。
 * 用这个标记把它和常规 `prop` 区分开，供 checker / renderer / CFA 精确识别。
 */
var CfirProperty.isCatchParameter: Boolean? by CfirDeclarationDataRegistry.data(IsCatchParameterPropertyKey)

/**
 * callable 所属 class lookup tag 的附加数据键。
 */
private object ContainingClassKey : CfirDeclarationDataKey()

/**
 * 从 callable symbol 的 dispatch receiver 中提取 class lookup tag。
 */
fun CfirCallableSymbol<*>.dispatchReceiverClassLookupTagOrNull(): ConeClassLikeLookupTag? =
    cfir.dispatchReceiverClassLookupTagOrNull()

/**
 * 从 callable 声明的 dispatch receiver 中提取 class lookup tag。
 */
fun CfirCallableDeclaration.dispatchReceiverClassLookupTagOrNull(): ConeClassLikeLookupTag? =
    dispatchReceiverClassTypeOrNull()?.lookupTag

/**
 * 从 callable 声明的 dispatch receiver 中提取 class-like 类型。
 */
fun CfirCallableDeclaration.dispatchReceiverClassTypeOrNull(): ConeClassLikeType? =
    if (dispatchReceiverType is ConeIntersectionType && isIntersectionOverride)
        baseForIntersectionOverride!!.dispatchReceiverClassTypeOrNull()
    else
        dispatchReceiverType as? ConeClassLikeType
