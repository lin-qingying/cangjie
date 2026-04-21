package org.cangnova.cangjie.analysis.api.cfir.symbols

import org.cangnova.cangjie.analysis.api.cfir.*

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.symbols.CaAnonymousFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFinalizerSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaMacroSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaNamedFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPatternBindingSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPatternVariableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertyAccessorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertyGetterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySetterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolModality
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolOrigin
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolVisibility
import org.cangnova.cangjie.analysis.api.symbols.CaTypeAliasSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaLocalVariableSymbol
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjFinalizer
import org.cangnova.cangjie.psi.CjFunctionLiteral
import org.cangnova.cangjie.psi.CjMacroDeclaration
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjProperty
import org.cangnova.cangjie.psi.CjPropertyAccessor
import org.cangnova.cangjie.psi.CjTypeAlias
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.psi.CjExtend
import org.cangnova.cangjie.psi.CjConstructor

/**
 * declaration 的公开位置语义由宿主声明结构决定。
 *
 * 这里统一收敛 file/class/property/extend/local 等位置判断，
 * 避免每个叶子 symbol 各自复制一份位置推导逻辑。
 */
internal fun CaCfirSession.locationForDeclaration(symbol: CaDeclarationSymbol): CaSymbolLocation = when (symbol) {
    is CaPropertyGetterSymbol,
    is CaPropertySetterSymbol,
    -> CaSymbolLocation.PROPERTY

    is CaAnonymousFunctionSymbol,
    is CaParameterSymbol,
    is CaPatternVariableSymbol,
    is CaPatternBindingSymbol,
    is CaTypeParameterSymbol,
    is CaLocalVariableSymbol,
    -> CaSymbolLocation.LOCAL

    else -> when (symbol.containingDeclaration) {
        is CaExtendSymbol -> CaSymbolLocation.EXTEND
        is CaClassSymbol -> CaSymbolLocation.CLASS
        is CaPropertySymbol -> CaSymbolLocation.PROPERTY
        else -> CaSymbolLocation.TOP_LEVEL
    }
}

internal fun <S : CaSymbol> CaCfirSession.getPublicSymbolByPsi(
    psi: PsiElement,
    symbolType: Class<S>,
): S? {
    val matches = symbolQueries.lookupSymbolsByPsi(psi)
        .map(::getPublicSymbol)
        .filter { symbol -> symbolType.isInstance(symbol) }
        .map(symbolType::cast)
    return matches.singleOrNull()
}

internal inline fun <reified S : CaSymbol> CaCfirSession.getPublicSymbolByPsi(psi: PsiElement): S? =
    getPublicSymbolByPsi(psi, S::class.java)

/**
 * 通过 PSI 宿主链恢复 containing declaration。
 *
 * 这层只负责 declaration 宿主导航，不承担 symbol 身份定义职责。
 */
internal fun CaCfirSession.findContainingDeclarationSymbol(psi: PsiElement?): CaSymbol? {
    var current = psi?.parent
    while (current != null) {
        val container = when (current) {
            is CjPropertyAccessor -> getPublicSymbolByPsi<CaPropertyAccessorSymbol>(current)
            is CjProperty -> getPublicSymbolByPsi<CaPropertySymbol>(current)
            is CjExtend -> getPublicSymbolByPsi<CaExtendSymbol>(current)
            is CjTypeAlias -> getPublicSymbolByPsi<CaTypeAliasSymbol>(current)
            is CjTypeStatement -> getPublicSymbolByPsi<CaClassSymbol>(current)
            is CjNamedFunction -> getPublicSymbolByPsi<CaNamedFunctionSymbol>(current)
            is CjFunctionLiteral -> getPublicSymbolByPsi<CaAnonymousFunctionSymbol>(current)
            is CjConstructor<*> -> getPublicSymbolByPsi<CaConstructorSymbol>(current)
            is CjFinalizer -> getPublicSymbolByPsi<CaFinalizerSymbol>(current)
            is CjMacroDeclaration -> getPublicSymbolByPsi<CaMacroSymbol>(current)
            is CjFile -> createFileSymbol(current)
            else -> null
        }
        if (container != null) return container
        current = current.parent
    }
    return null
}

internal fun CfirDeclarationOrigin.asPublicOrigin(): CaSymbolOrigin = when (this) {
    CfirDeclarationOrigin.Source -> CaSymbolOrigin.SOURCE
    CfirDeclarationOrigin.Library -> CaSymbolOrigin.LIBRARY
    CfirDeclarationOrigin.ImplicitDefault -> CaSymbolOrigin.IMPLICIT_DEFAULT
    CfirDeclarationOrigin.GenericInstantiation -> CaSymbolOrigin.GENERIC_INSTANTIATION
    CfirDeclarationOrigin.Extension -> CaSymbolOrigin.EXTENSION
    CfirDeclarationOrigin.SamConstructor -> CaSymbolOrigin.SAM_CONSTRUCTOR
    CfirDeclarationOrigin.IntersectionOverride -> CaSymbolOrigin.SYNTHETIC
    CfirDeclarationOrigin.SubstitutionOverride.DeclarationSite -> CaSymbolOrigin.SUBSTITUTION_OVERRIDE_DECLARATION_SITE
    CfirDeclarationOrigin.SubstitutionOverride.CallSite -> CaSymbolOrigin.SUBSTITUTION_OVERRIDE_CALL_SITE
    is CfirDeclarationOrigin.Synthetic -> CaSymbolOrigin.SYNTHETIC
}

internal fun Visibility.asPublicVisibility(): CaSymbolVisibility = when (this) {
    Visibilities.Private -> CaSymbolVisibility.PRIVATE
    Visibilities.PrivateToThis -> CaSymbolVisibility.PRIVATE_TO_THIS
    Visibilities.Protected -> CaSymbolVisibility.PROTECTED
    Visibilities.Internal -> CaSymbolVisibility.INTERNAL
    Visibilities.Public -> CaSymbolVisibility.PUBLIC
    Visibilities.Local -> CaSymbolVisibility.LOCAL
    else -> CaSymbolVisibility.UNKNOWN
}

internal fun Modality.asPublicModality(): CaSymbolModality = when (this) {
    Modality.FINAL -> CaSymbolModality.FINAL
    Modality.SEALED -> CaSymbolModality.SEALED
    Modality.OPEN -> CaSymbolModality.OPEN
    Modality.ABSTRACT -> CaSymbolModality.ABSTRACT
}
