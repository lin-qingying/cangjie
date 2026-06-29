package org.cangnova.cangjie.chir.core.symbol

import org.cangnova.cangjie.chir.core.identity.ChirSemanticId

/**
 * CHIR 符号表接口。
 */
interface ChirSymbolTable {
    /**
     * 声明一个符号。
     */
    fun declare(symbol: ChirSymbol)

    /**
     * 按符号名称解析符号。
     */
    fun resolveByName(name: String): ChirSymbol?

    /**
     * 按声明语义标识解析符号。
     */
    fun resolveByDeclarationId(declarationId: ChirSemanticId): ChirSymbol?
}

/**
 * 默认内存符号表实现。
 */
class DefaultChirSymbolTable : ChirSymbolTable {
    /**
     * 符号语义标识索引。
     */
    private val byId = LinkedHashMap<ChirSemanticId, ChirSymbol>()

    /**
     * 符号名称索引。
     */
    private val byName = LinkedHashMap<String, ChirSymbol>()

    /**
     * 声明语义标识索引。
     */
    private val byDeclaration = LinkedHashMap<ChirSemanticId, ChirSymbol>()

    /**
     * 声明符号并维护所有索引。
     */
    override fun declare(symbol: ChirSymbol) {
        require(symbol.semanticId !in byId) { "duplicate symbol id: ${symbol.semanticId}" }
        require(symbol.name !in byName) { "duplicate symbol name: ${symbol.name}" }
        require(symbol.declarationId !in byDeclaration) { "declaration already has symbol: ${symbol.declarationId}" }
        byId[symbol.semanticId] = symbol
        byName[symbol.name] = symbol
        byDeclaration[symbol.declarationId] = symbol
    }

    /**
     * 按符号名称解析符号。
     */
    override fun resolveByName(name: String): ChirSymbol? = byName[name]

    /**
     * 按声明语义标识解析符号。
     */
    override fun resolveByDeclarationId(declarationId: ChirSemanticId): ChirSymbol? = byDeclaration[declarationId]
}
