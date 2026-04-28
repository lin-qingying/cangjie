package org.cangnova.cangjie.analysis.api.cfir.symbols

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolOrigin
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.descriptors.Visibilities


internal interface CaCfirSymbol<out S : CfirBasedSymbol<*>> : CaSymbol, CaLifetimeOwner {
    /**
     * The underlying [CfirBasedSymbol] which is used to provide other property implementations.
     */
    val cfirSymbol: S

    val analysisSession: CaCfirSession
    val builder: CaSymbolByCfirBuilder get() = analysisSession.cfirSymbolBuilder

    override val token: CaLifetimeToken get() = analysisSession.token
    override val origin: CaSymbolOrigin get() = withValidityAssertion { symbolOrigin() }
}

internal fun CaCfirSymbol<*>.symbolOrigin(): CaSymbolOrigin = cfirSymbol.cfir.cjSymbolOrigin()

internal fun CfirDeclaration.cjSymbolOrigin(): CaSymbolOrigin = origin.asPublicOrigin()

internal fun CaCfirSymbol<*>.getSymbolKind(): CaSymbolLocation {
    val cfirSymbol = cfirSymbol
    return when {
        cfirSymbol is CfirClassLikeSymbol<*> -> CaSymbolLocation.TOP_LEVEL
        else -> CaSymbolLocation.LOCAL
    }
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

internal fun Visibility.asPublicVisibility() = when (this) {
    Visibilities.Private -> org.cangnova.cangjie.analysis.api.symbols.CaSymbolVisibility.PRIVATE
    Visibilities.PrivateToThis -> org.cangnova.cangjie.analysis.api.symbols.CaSymbolVisibility.PRIVATE_TO_THIS
    Visibilities.Protected -> org.cangnova.cangjie.analysis.api.symbols.CaSymbolVisibility.PROTECTED
    Visibilities.Internal -> org.cangnova.cangjie.analysis.api.symbols.CaSymbolVisibility.INTERNAL
    Visibilities.Public -> org.cangnova.cangjie.analysis.api.symbols.CaSymbolVisibility.PUBLIC
    Visibilities.Local -> org.cangnova.cangjie.analysis.api.symbols.CaSymbolVisibility.LOCAL
    else -> org.cangnova.cangjie.analysis.api.symbols.CaSymbolVisibility.UNKNOWN
}

internal fun Modality.asPublicModality() = when (this) {
    Modality.FINAL -> org.cangnova.cangjie.analysis.api.symbols.CaSymbolModality.FINAL
    Modality.SEALED -> org.cangnova.cangjie.analysis.api.symbols.CaSymbolModality.SEALED
    Modality.OPEN -> org.cangnova.cangjie.analysis.api.symbols.CaSymbolModality.OPEN
    Modality.ABSTRACT -> org.cangnova.cangjie.analysis.api.symbols.CaSymbolModality.ABSTRACT
}
