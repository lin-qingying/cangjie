package org.cangnova.cangjie.cfir.resolve.services

import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.resolve.providers.CfirDirectSupertypeProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSuperTypeGraphEdge
import org.cangnova.cangjie.cfir.resolve.providers.CfirSuperTypeGraphNode
import org.cangnova.cangjie.cfir.resolve.providers.mergeSuperTypes
import org.cangnova.cangjie.cfir.resolve.providers.stableKey
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.name.ClassId

/**
 * 会话级超类型图存储。
 *
 * 该组件同时服务于 supertype resolve 阶段的写入和后续 provider 查询，
 * 保证声明继承与 extend 继承在同一直接超类型视图中合并。
 */
class CfirSuperTypeGraphStore : CfirSessionComponent, CfirDirectSupertypeProvider {
    /**
     * classId 到声明超类型边的索引。
     */
    private val declaredSuperTypesByOwner = mutableMapOf<ClassId, List<CfirSuperTypeGraphEdge>>()
    /**
     * classId 到 extend 追加超类型边的索引。
     */
    private val extendedSuperTypesByOwner = mutableMapOf<ClassId, List<CfirSuperTypeGraphEdge>>()

    /**
     * 记录 class-like 声明头中解析出的直接超类型。
     */
    fun recordDeclared(owner: CfirClassLikeDeclaration, superTypes: List<CfirSuperTypeGraphEdge>) {
        val ownerSymbol = owner.symbol as? CfirClassLikeSymbol<*> ?: return
        declaredSuperTypesByOwner[ownerSymbol.classId] = superTypes.distinctBy(CfirSuperTypeGraphEdge::stableKey)
    }

    /**
     * 记录 extend 语义追加到指定 classId 的直接超类型。
     */
    fun recordExtended(ownerClassId: ClassId, superTypes: List<CfirSuperTypeGraphEdge>) {
        val existing = extendedSuperTypesByOwner[ownerClassId].orEmpty()
        extendedSuperTypesByOwner[ownerClassId] =
            mergeSuperTypes(existing, superTypes).distinctBy(CfirSuperTypeGraphEdge::stableKey)
    }

    /**
     * 清空所有 extend 追加的超类型边。
     */
    fun clearExtended() {
        extendedSuperTypesByOwner.clear()
    }

    /**
     * 获取指定 classId 的超类型图节点。
     */
    fun getNode(ownerClassId: ClassId): CfirSuperTypeGraphNode? {
        val declared = declaredSuperTypesByOwner[ownerClassId].orEmpty()
        val extended = extendedSuperTypesByOwner[ownerClassId].orEmpty()
        if (declared.isEmpty() && extended.isEmpty()) return null
        return CfirSuperTypeGraphNode(
            ownerClassId = ownerClassId,
            declaredSuperTypes = declared,
            extendedSuperTypes = extended,
        )
    }

    /**
     * 获取指定 classId 的直接超类型边。
     */
    override fun getDirectSuperTypeEdges(ownerClassId: ClassId): List<CfirSuperTypeGraphEdge> =
        getNode(ownerClassId)?.superTypes.orEmpty()

    /**
     * Provider 接口要求的直接超类型引用查询。
     */
    override fun getDirectSuperTypes(ownerClassId: ClassId): List<CfirResolvedTypeRef> =
        getDirectSuperTypeEdges(ownerClassId).map(CfirSuperTypeGraphEdge::typeRef)
}
