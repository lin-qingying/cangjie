package org.cangnova.cangjie.cfir.resolve.services

import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.resolve.providers.CfirDirectSupertypeProvider
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.name.ClassId

/**
 * 超类型图中的一条边。
 *
 * 边记录源码类型引用、稳定渲染文本、解析到的 class-like 符号以及可选的 extend 来源。
 */
data class CfirSuperTypeGraphEdge(
    /**
     * 已解析的超类型引用。
     */
    val typeRef: CfirResolvedTypeRef,
    /**
     * 超类型的稳定渲染文本，用于无法获得 classId 时去重。
     */
    val renderedType: String,
    /**
     * 超类型解析到的 class-like 符号。
     */
    val resolvedClassSymbol: CfirClassLikeSymbol<*>?,
    /**
     * 贡献该超类型边的 extend 声明；声明自身 supertype 边为空。
     */
    val sourceExtend: CfirExtend? = null,
)

/**
 * 单个 class-like 声明的超类型图节点。
 */
data class CfirSuperTypeGraphNode(
    /**
     * 节点所属 class-like 的 classId。
     */
    val ownerClassId: ClassId,
    /**
     * 声明头中直接写出的超类型边。
     */
    val declaredSuperTypes: List<CfirSuperTypeGraphEdge>,
    /**
     * extend 机制追加的超类型边。
     */
    val extendedSuperTypes: List<CfirSuperTypeGraphEdge>,
) {
    /**
     * 声明超类型与 extend 超类型合并后的直接超类型边。
     */
    val superTypes: List<CfirSuperTypeGraphEdge>
        get() = mergeSuperTypes(declaredSuperTypes, extendedSuperTypes)
}

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
    fun getDirectSuperTypeEdges(ownerClassId: ClassId): List<CfirSuperTypeGraphEdge> =
        getNode(ownerClassId)?.superTypes.orEmpty()

    /**
     * Provider 接口要求的直接超类型引用查询。
     */
    override fun getDirectSuperTypes(ownerClassId: ClassId): List<CfirResolvedTypeRef> =
        getDirectSuperTypeEdges(ownerClassId).map(CfirSuperTypeGraphEdge::typeRef)
}

/**
 * 合并声明超类型和 extend 超类型，并按稳定 key 去重。
 */
private fun mergeSuperTypes(
    declared: List<CfirSuperTypeGraphEdge>,
    extended: List<CfirSuperTypeGraphEdge>,
): List<CfirSuperTypeGraphEdge> {
    if (declared.isEmpty()) return extended
    if (extended.isEmpty()) return declared

    val merged = LinkedHashMap<String, CfirSuperTypeGraphEdge>()
    for (edge in declared) {
        merged.putIfAbsent(edge.stableKey(), edge)
    }
    for (edge in extended) {
        merged.putIfAbsent(edge.stableKey(), edge)
    }
    return merged.values.toList()
}

/**
 * 计算超类型边的稳定去重 key。
 */
private fun CfirSuperTypeGraphEdge.stableKey(): String {
    return resolvedClassSymbol?.classId?.asString() ?: renderedType
}
