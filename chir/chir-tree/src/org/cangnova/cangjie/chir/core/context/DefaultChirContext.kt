package org.cangnova.cangjie.chir.core.context

import org.cangnova.cangjie.chir.core.declaration.ChirDeclaration
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.chir.core.symbol.ChirSymbol
import org.cangnova.cangjie.chir.core.type.ChirType

/**
 * 基于内存索引的默认 CHIR 上下文实现。
 */
class DefaultChirContext : ChirContext {
    /**
     * 包语义标识到包节点的索引。
     */
    private val packageIndex = LinkedHashMap<ChirSemanticId, ChirPackage>()

    /**
     * 模块语义标识到模块节点的索引。
     */
    private val moduleIndex = LinkedHashMap<ChirSemanticId, ChirModule>()

    /**
     * 声明语义标识到声明节点的索引。
     */
    private val declarationIndex = LinkedHashMap<ChirSemanticId, ChirDeclaration>()

    /**
     * 类型语义标识到类型节点的索引。
     */
    private val typeIndex = LinkedHashMap<ChirSemanticId, ChirType>()

    /**
     * 符号语义标识到符号的索引。
     */
    private val symbolIndex = LinkedHashMap<ChirSemanticId, ChirSymbol>()

    /**
     * 当前上下文中的包快照。
     */
    override val packages: Collection<ChirPackage>
        get() = packageIndex.values.toList()

    /**
     * 当前上下文中的模块快照。
     */
    override val modules: Collection<ChirModule>
        get() = moduleIndex.values.toList()

    /**
     * 当前上下文中的声明快照。
     */
    override val declarations: Collection<ChirDeclaration>
        get() = declarationIndex.values.toList()

    /**
     * 当前上下文中的类型快照。
     */
    override val types: Collection<ChirType>
        get() = typeIndex.values.toList()

    /**
     * 当前上下文中的符号快照。
     */
    override val symbols: Collection<ChirSymbol>
        get() = symbolIndex.values.toList()

    /**
     * 注册包节点。
     */
    override fun registerPackage(chirPackage: ChirPackage) {
        checkDuplicate(packageIndex, chirPackage.semanticId, "package")
        packageIndex[chirPackage.semanticId] = chirPackage
    }

    /**
     * 注册模块节点。
     */
    override fun registerModule(module: ChirModule) {
        checkDuplicate(moduleIndex, module.semanticId, "module")
        moduleIndex[module.semanticId] = module
    }

    /**
     * 注册声明节点。
     */
    override fun registerDeclaration(declaration: ChirDeclaration) {
        checkDuplicate(declarationIndex, declaration.semanticId, "declaration")
        declarationIndex[declaration.semanticId] = declaration
    }

    /**
     * 注册类型节点。
     */
    override fun registerType(typeId: ChirSemanticId, type: ChirType) {
        checkDuplicate(typeIndex, typeId, "type")
        typeIndex[typeId] = type
    }

    /**
     * 注册符号。
     */
    override fun registerSymbol(symbol: ChirSymbol) {
        checkDuplicate(symbolIndex, symbol.semanticId, "symbol")
        symbolIndex[symbol.semanticId] = symbol
    }

    /**
     * 按语义标识查找包节点。
     */
    override fun findPackage(id: ChirSemanticId): ChirPackage? = packageIndex[id]

    /**
     * 按语义标识查找模块节点。
     */
    override fun findModule(id: ChirSemanticId): ChirModule? = moduleIndex[id]

    /**
     * 按语义标识查找声明节点。
     */
    override fun findDeclaration(id: ChirSemanticId): ChirDeclaration? = declarationIndex[id]

    /**
     * 按语义标识查找类型节点。
     */
    override fun findType(id: ChirSemanticId): ChirType? = typeIndex[id]

    /**
     * 按语义标识查找符号。
     */
    override fun findSymbol(id: ChirSemanticId): ChirSymbol? = symbolIndex[id]

    /**
     * 校验指定 [id] 在索引中尚未存在。
     */
    private fun <T> checkDuplicate(index: Map<ChirSemanticId, T>, id: ChirSemanticId, kind: String) {
        require(id !in index) { "duplicate $kind id: $id" }
    }
}
