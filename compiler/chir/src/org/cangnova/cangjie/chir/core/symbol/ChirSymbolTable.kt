package org.cangnova.cangjie.chir.core.symbol

import org.cangnova.cangjie.chir.core.identity.ChirSemanticId

interface ChirSymbolTable {
    fun declare(symbol: ChirSymbol)
    fun resolveByName(name: String): ChirSymbol?
    fun resolveByDeclarationId(declarationId: ChirSemanticId): ChirSymbol?
}

class DefaultChirSymbolTable : ChirSymbolTable {
    private val byId = LinkedHashMap<ChirSemanticId, ChirSymbol>()
    private val byName = LinkedHashMap<String, ChirSymbol>()
    private val byDeclaration = LinkedHashMap<ChirSemanticId, ChirSymbol>()

    override fun declare(symbol: ChirSymbol) {
        require(symbol.semanticId !in byId) { "duplicate symbol id: ${symbol.semanticId}" }
        require(symbol.name !in byName) { "duplicate symbol name: ${symbol.name}" }
        require(symbol.declarationId !in byDeclaration) { "declaration already has symbol: ${symbol.declarationId}" }
        byId[symbol.semanticId] = symbol
        byName[symbol.name] = symbol
        byDeclaration[symbol.declarationId] = symbol
    }

    override fun resolveByName(name: String): ChirSymbol? = byName[name]

    override fun resolveByDeclarationId(declarationId: ChirSemanticId): ChirSymbol? = byDeclaration[declarationId]
}
