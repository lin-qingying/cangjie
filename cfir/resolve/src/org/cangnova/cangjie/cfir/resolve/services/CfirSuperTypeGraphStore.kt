package org.cangnova.cangjie.cfir.resolve.services

import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.isInReferenceCycle
import org.cangnova.cangjie.cfir.resolve.providers.CfirDirectSupertypeProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirCompletedSupertypeProduct
import org.cangnova.cangjie.cfir.resolve.providers.CfirSuperTypeGraphEdge
import org.cangnova.cangjie.cfir.resolve.providers.CfirSuperTypeGraphNode
import org.cangnova.cangjie.cfir.resolve.providers.mergeSuperTypes
import org.cangnova.cangjie.cfir.resolve.providers.stableKey
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.name.ClassId

/**
 * 会话级超类型图的计算与完成发布。
 *
 * 预采集和断环使用工作拓扑；普通查询只读取完成快照。版本覆盖完整引用、真实循环状态
 * 和来源边，不能以普通成员投影过滤后的边集合代替完成产品。
 */
class CfirSuperTypeGraphStore : CfirSessionComponent, CfirDirectSupertypeProvider {
    override var modificationCount: Long = 0
        private set

    private data class DeclarationProduct(
        val refs: List<CfirTypeRef>,
        val isInReferenceCycle: Boolean,
        val edges: List<CfirSuperTypeGraphEdge>,
    )

    private val declarationProducts = mutableMapOf<CfirClassLikeSymbol<*>, DeclarationProduct>()
    private val extendedSuperTypesByOwner = mutableMapOf<CfirClassLikeSymbol<*>, List<CfirSuperTypeGraphEdge>>()

    private var publishedDeclarations: Map<CfirClassLikeSymbol<*>, DeclarationProduct> = emptyMap()
    private var publishedExtendedEdges: Map<CfirClassLikeSymbol<*>, List<CfirSuperTypeGraphEdge>> = emptyMap()
    private var completedProducts: Map<CfirClassLikeSymbol<*>, CfirCompletedSupertypeProduct> = emptyMap()

    /** 记录计算中的完整声明引用及其普通父边投影；由完成 owner 统一发布。 */
    fun recordDeclared(
        owner: CfirClassLikeDeclaration,
        resolvedRefs: List<CfirTypeRef>,
        superTypes: List<CfirSuperTypeGraphEdge>,
    ) {
        val edges = superTypes.distinctBy(CfirSuperTypeGraphEdge::stableKey)
        declarationProducts[owner.symbol] = DeclarationProduct(resolvedRefs.toList(), owner.isInReferenceCycle, edges)
    }

    /** extend 预采集保留工作边，采集中间状态不成为新的完成版本。 */
    fun recordExtended(ownerSymbol: CfirClassLikeSymbol<*>, superTypes: List<CfirSuperTypeGraphEdge>) {
        extendedSuperTypesByOwner[ownerSymbol] = mergeSuperTypes(
            extendedSuperTypesByOwner[ownerSymbol].orEmpty(), superTypes,
        ).distinctBy(CfirSuperTypeGraphEdge::stableKey)
    }

    /** 同步工作拓扑中的旧 extend 引用，保留原目标和来源供原生拓扑及诊断查询。 */
    fun replaceExtendedTypeRef(extend: CfirExtend, original: CfirResolvedTypeRef, replacement: CfirResolvedTypeRef) {
        if (original === replacement) return
        for ((owner, edges) in extendedSuperTypesByOwner.toMap()) {
            extendedSuperTypesByOwner[owner] = edges.map { edge ->
                if (edge.sourceExtend === extend && edge.typeRef === original) edge.copy(typeRef = replacement) else edge
            }
        }
    }

    /** 开始重建工作 extend 集合；普通查询仍使用上一份完成快照。 */
    fun clearExtended() {
        extendedSuperTypesByOwner.clear()
    }

    /** AST、引用与循环状态完成后统一发布；相同完整产品重发不递增。 */
    fun publishCompletedProducts() {
        if (publishedDeclarations == declarationProducts &&
            publishedExtendedEdges == extendedSuperTypesByOwner
        ) return

        publishedDeclarations = declarationProducts.toMap()
        publishedExtendedEdges = extendedSuperTypesByOwner.toMap()
        // marker-only 循环发布保留原始引用和状态；尚未完成的语法引用不授予完整父图产品。
        completedProducts = publishedDeclarations.filterValues { product ->
            product.refs.all { it is CfirResolvedTypeRef }
        }.mapValues { (symbol, product) ->
            CfirCompletedSupertypeProduct(symbol, product.refs, product.edges,
                publishedExtendedEdges[symbol].orEmpty(), product.isInReferenceCycle)
        }
        modificationCount++
    }

    /** 断环计算读取保留原来源的工作拓扑，不能改用已剪边的普通成员闭包。 */
    fun getNode(ownerClassId: ClassId): CfirSuperTypeGraphNode? {
        val owner = uniqueOwner(ownerClassId, declarationProducts.keys + extendedSuperTypesByOwner.keys) ?: return null
        return getNode(owner)
    }

    /** 工作拓扑也保留真实 owner，两个同名声明不会串换父边。 */
    fun getNode(owner: CfirClassLikeSymbol<*>): CfirSuperTypeGraphNode? {
        val declared = declarationProducts[owner]?.edges.orEmpty()
        val extended = extendedSuperTypesByOwner[owner].orEmpty()
        if (declared.isEmpty() && extended.isEmpty()) return null
        return CfirSuperTypeGraphNode(owner.classId, declared, extended)
    }

    override fun getDirectSuperTypeEdges(ownerClassId: ClassId): List<CfirSuperTypeGraphEdge> {
        val owner = uniqueOwner(ownerClassId, publishedDeclarations.keys + publishedExtendedEdges.keys) ?: return emptyList()
        return mergeSuperTypes(publishedDeclarations[owner]?.edges.orEmpty(), publishedExtendedEdges[owner].orEmpty())
    }

    /** 名称接口只适用于唯一 owner；有歧义时调用方必须使用原 symbol 的产品接口。 */
    private fun uniqueOwner(classId: ClassId, symbols: Collection<CfirClassLikeSymbol<*>>): CfirClassLikeSymbol<*>? {
        val matching = symbols.filter { it.classId == classId }.distinct()
        check(matching.size <= 1) { "ClassId does not identify a unique supertype owner: $classId" }
        return matching.singleOrNull()
    }

    override fun getDirectSuperTypes(ownerClassId: ClassId): List<CfirResolvedTypeRef> =
        getDirectSuperTypeEdges(ownerClassId).map(CfirSuperTypeGraphEdge::typeRef)

    override fun getCompletedSupertypeProduct(ownerSymbol: CfirClassLikeSymbol<*>): CfirCompletedSupertypeProduct? =
        completedProducts[ownerSymbol]

    override fun getPublishedReferenceCycleState(ownerSymbol: CfirClassLikeSymbol<*>): Boolean? =
        publishedDeclarations[ownerSymbol]?.takeIf { it.isInReferenceCycle || it.refs.all { ref -> ref is CfirResolvedTypeRef } }
            ?.isInReferenceCycle
}
