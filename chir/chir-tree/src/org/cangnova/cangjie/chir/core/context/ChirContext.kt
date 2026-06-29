package org.cangnova.cangjie.chir.core.context

import org.cangnova.cangjie.chir.core.declaration.ChirDeclaration
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.chir.core.symbol.ChirSymbol
import org.cangnova.cangjie.chir.core.type.ChirType

/**
 * CHIR 上下文的只读视图。
 */
interface ChirReadOnlyContext {
    /**
     * 当前上下文中的包集合快照。
     */
    val packages: Collection<ChirPackage>

    /**
     * 当前上下文中的模块集合快照。
     */
    val modules: Collection<ChirModule>

    /**
     * 当前上下文中的声明集合快照。
     */
    val declarations: Collection<ChirDeclaration>

    /**
     * 当前上下文中的类型集合快照。
     */
    val types: Collection<ChirType>

    /**
     * 当前上下文中的符号集合快照。
     */
    val symbols: Collection<ChirSymbol>

    /**
     * 按语义标识查找包。
     */
    fun findPackage(id: ChirSemanticId): ChirPackage?

    /**
     * 按语义标识查找模块。
     */
    fun findModule(id: ChirSemanticId): ChirModule?

    /**
     * 按语义标识查找声明。
     */
    fun findDeclaration(id: ChirSemanticId): ChirDeclaration?

    /**
     * 按语义标识查找类型。
     */
    fun findType(id: ChirSemanticId): ChirType?

    /**
     * 按语义标识查找符号。
     */
    fun findSymbol(id: ChirSemanticId): ChirSymbol?
}

/**
 * 可写 CHIR 上下文。
 */
interface ChirContext : ChirReadOnlyContext {

    /**
     * 注册包节点。
     */
    fun registerPackage(chirPackage: ChirPackage)

    /**
     * 注册模块节点。
     */
    fun registerModule(module: ChirModule)

    /**
     * 注册声明节点。
     */
    fun registerDeclaration(declaration: ChirDeclaration)

    /**
     * 注册类型节点。
     */
    fun registerType(typeId: ChirSemanticId, type: ChirType)

    /**
     * 注册符号。
     */
    fun registerSymbol(symbol: ChirSymbol)
}
