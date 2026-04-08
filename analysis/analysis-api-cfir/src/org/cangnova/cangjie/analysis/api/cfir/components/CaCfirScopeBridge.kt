package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.scopes.CaScope
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassifierSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.name
import org.cangnova.cangjie.name.Name

internal class CaCfirScopeImpl(
    private val indexedNames: Set<Name>,
    private val eagerSymbols: List<CaSymbol>,
    override val token: CaLifetimeToken,
    private val symbolLookup: (Name) -> List<CaSymbol>,
    private val callableLookup: (Name) -> List<CaCallableSymbol>,
    private val classifierLookup: (Name) -> List<CaClassifierSymbol>,
) : CaScope {
    private val cachedSymbolsByName = linkedMapOf<Name, List<CaSymbol>>()
    private val cachedCallableSymbolsByName = linkedMapOf<Name, List<CaCallableSymbol>>()
    private val cachedClassifierSymbolsByName = linkedMapOf<Name, List<CaClassifierSymbol>>()

    override val availableNames: Set<Name>
        get() = indexedNames

    override val symbols: List<CaSymbol> by lazy(LazyThreadSafetyMode.NONE) {
        buildList {
            addAll(eagerSymbols)
            indexedNames.forEach { name -> addAll(getSymbols(name)) }
        }.distinctBy { symbol -> symbol.completionDecisionKey() }
    }

    override fun getSymbols(name: Name): List<CaSymbol> {
        return cachedSymbolsByName.getOrPut(name) {
            val eagerSymbolsByName = eagerSymbols.filter { symbol -> symbol.name == name }
            (eagerSymbolsByName + symbolLookup(name)).distinctBy { symbol -> symbol.completionDecisionKey() }
        }
    }

    override fun getCallableSymbols(name: Name): List<CaCallableSymbol> {
        return cachedCallableSymbolsByName.getOrPut(name) {
            callableLookup(name).distinctBy { symbol -> symbol.completionDecisionKey() }
        }
    }

    override fun getClassifierSymbols(name: Name): List<CaClassifierSymbol> {
        return cachedClassifierSymbolsByName.getOrPut(name) {
            classifierLookup(name).distinctBy { symbol -> symbol.completionDecisionKey() }
        }
    }
}
