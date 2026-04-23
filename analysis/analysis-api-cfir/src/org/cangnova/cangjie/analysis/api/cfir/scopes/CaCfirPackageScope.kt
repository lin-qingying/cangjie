package org.cangnova.cangjie.analysis.api.cfir.scopes

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.scopes.CaScope
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassifierSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.cfir.scopes.impl.CfirPackageMemberScope
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

@OptIn(CaPlatformInterface::class, CaExperimentalApi::class)
internal class CaCfirPackageScope(
    private val fqName: FqName,
    private val analysisSession: CaCfirSession,
) : CaScope {
    override val token: CaLifetimeToken get() = analysisSession.token

    private val firScope: CfirPackageMemberScope by lazy(LazyThreadSafetyMode.PUBLICATION) {
        CfirPackageMemberScope(fqName, analysisSession.cfirSession)
    }

    override fun getPossibleCallableNames(): Set<Name> = withValidityAssertion {
        analysisSession.useSiteScopeDeclarationProvider.getTopLevelCallableNamesInPackage(fqName)
    }

    override fun getPossibleClassifierNames(): Set<Name> = withValidityAssertion {
        analysisSession.useSiteScopeDeclarationProvider.getTopLevelCangJieClassLikeDeclarationNamesInPackage(fqName)
    }

    override fun callables(nameFilter: (Name) -> Boolean): Sequence<CaCallableSymbol> = withValidityAssertion {
        firScope.getCallableSymbols(getPossibleCallableNames().filter(nameFilter), analysisSession.cfirSymbolBuilder)
    }

    override fun callables(names: Collection<Name>): Sequence<CaCallableSymbol> = withValidityAssertion {
        firScope.getCallableSymbols(names, analysisSession.cfirSymbolBuilder)
    }

    override fun classifiers(nameFilter: (Name) -> Boolean): Sequence<CaClassifierSymbol> = withValidityAssertion {
        firScope.getClassifierSymbols(getPossibleClassifierNames().filter(nameFilter), analysisSession.cfirSymbolBuilder)
    }

    override fun classifiers(names: Collection<Name>): Sequence<CaClassifierSymbol> = withValidityAssertion {
        firScope.getClassifierSymbols(names, analysisSession.cfirSymbolBuilder)
    }

    override val constructors: Sequence<CaConstructorSymbol>
        get() = withValidityAssertion { emptySequence() }

    override fun getPackageSymbols(nameFilter: (Name) -> Boolean): Sequence<CaPackageSymbol> = withValidityAssertion {
        sequence {
            analysisSession.useSitePackageProvider.getSubpackageNames(fqName).forEach { name ->
                if (nameFilter(name)) {
                    yield(analysisSession.cfirSymbolBuilder.createPackageSymbol(fqName.child(name)))
                }
            }
        }
    }
}
