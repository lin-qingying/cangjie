package org.cangnova.cangjie.chir.core.context

import org.cangnova.cangjie.chir.core.declaration.ChirDeclaration
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.chir.core.symbol.ChirSymbol
import org.cangnova.cangjie.chir.core.type.ChirType

interface ChirContext {
    val packages: Collection<ChirPackage>
    val modules: Collection<ChirModule>
    val declarations: Collection<ChirDeclaration>
    val types: Collection<ChirType>
    val symbols: Collection<ChirSymbol>

    fun registerPackage(chirPackage: ChirPackage)
    fun registerModule(module: ChirModule)
    fun registerDeclaration(declaration: ChirDeclaration)
    fun registerType(typeId: ChirSemanticId, type: ChirType)
    fun registerSymbol(symbol: ChirSymbol)

    fun findPackage(id: ChirSemanticId): ChirPackage?
    fun findModule(id: ChirSemanticId): ChirModule?
    fun findDeclaration(id: ChirSemanticId): ChirDeclaration?
    fun findType(id: ChirSemanticId): ChirType?
    fun findSymbol(id: ChirSemanticId): ChirSymbol?
}
