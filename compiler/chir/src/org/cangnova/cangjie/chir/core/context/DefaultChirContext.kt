package org.cangnova.cangjie.chir.core.context

import org.cangnova.cangjie.chir.core.declaration.ChirDeclaration
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.chir.core.symbol.ChirSymbol
import org.cangnova.cangjie.chir.core.type.ChirType

class DefaultChirContext : ChirContext {
    private val packageIndex = LinkedHashMap<ChirSemanticId, ChirPackage>()
    private val moduleIndex = LinkedHashMap<ChirSemanticId, ChirModule>()
    private val declarationIndex = LinkedHashMap<ChirSemanticId, ChirDeclaration>()
    private val typeIndex = LinkedHashMap<ChirSemanticId, ChirType>()
    private val symbolIndex = LinkedHashMap<ChirSemanticId, ChirSymbol>()

    override val packages: Collection<ChirPackage>
        get() = packageIndex.values

    override val modules: Collection<ChirModule>
        get() = moduleIndex.values

    override val declarations: Collection<ChirDeclaration>
        get() = declarationIndex.values

    override val types: Collection<ChirType>
        get() = typeIndex.values

    override val symbols: Collection<ChirSymbol>
        get() = symbolIndex.values

    override fun registerPackage(chirPackage: ChirPackage) {
        checkDuplicate(packageIndex, chirPackage.semanticId, "package")
        packageIndex[chirPackage.semanticId] = chirPackage
    }

    override fun registerModule(module: ChirModule) {
        checkDuplicate(moduleIndex, module.semanticId, "module")
        moduleIndex[module.semanticId] = module
    }

    override fun registerDeclaration(declaration: ChirDeclaration) {
        checkDuplicate(declarationIndex, declaration.semanticId, "declaration")
        declarationIndex[declaration.semanticId] = declaration
    }

    override fun registerType(typeId: ChirSemanticId, type: ChirType) {
        checkDuplicate(typeIndex, typeId, "type")
        typeIndex[typeId] = type
    }

    override fun registerSymbol(symbol: ChirSymbol) {
        checkDuplicate(symbolIndex, symbol.semanticId, "symbol")
        symbolIndex[symbol.semanticId] = symbol
    }

    override fun findPackage(id: ChirSemanticId): ChirPackage? = packageIndex[id]

    override fun findModule(id: ChirSemanticId): ChirModule? = moduleIndex[id]

    override fun findDeclaration(id: ChirSemanticId): ChirDeclaration? = declarationIndex[id]

    override fun findType(id: ChirSemanticId): ChirType? = typeIndex[id]

    override fun findSymbol(id: ChirSemanticId): ChirSymbol? = symbolIndex[id]

    private fun <T> checkDuplicate(index: Map<ChirSemanticId, T>, id: ChirSemanticId, kind: String) {
        require(id !in index) { "duplicate $kind id: $id" }
    }
}
