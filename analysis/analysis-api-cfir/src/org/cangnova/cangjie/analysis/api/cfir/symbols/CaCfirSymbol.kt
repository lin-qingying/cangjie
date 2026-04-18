package org.cangnova.cangjie.analysis.api.cfir.symbols

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolOrigin
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol


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



internal tailrec fun CfirDeclaration.cjSymbolOrigin(): CaSymbolOrigin = when (origin) {
    CfirDeclarationOrigin.Source -> {
        when (source?.kind) {

            else -> CaSymbolOrigin.SOURCE
        }
    }

    CfirDeclarationOrigin.Library  -> CaSymbolOrigin.LIBRARY
    CfirDeclarationOrigin.SamConstructor -> CaSymbolOrigin.SAM_CONSTRUCTOR



    CfirDeclarationOrigin.Extension -> TODO()
    CfirDeclarationOrigin.GenericInstantiation -> TODO()
    CfirDeclarationOrigin.ImplicitDefault -> TODO()
    CfirDeclarationOrigin.SubstitutionOverride.CallSite -> TODO()
    CfirDeclarationOrigin.SubstitutionOverride.DeclarationSite -> TODO()
    CfirDeclarationOrigin.Synthetic.Default -> TODO()
    CfirDeclarationOrigin.Synthetic.Error -> TODO()
    CfirDeclarationOrigin.Synthetic.FakeFunction -> TODO()
}
