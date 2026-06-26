package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
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
 * 按声明 [ClassId] 查询直接父类型的 session 组件。
 *
 * 该接口返回的是声明上原始记录的 resolved type ref，尚未根据某个 use-site 的实际类型实参重写。
 */
interface CfirDirectSupertypeProvider : CfirSessionComponent {
    /**
     * 返回 [ownerClassId] 声明中直接写出的父类型列表。
     */
    fun getDirectSuperTypes(ownerClassId: ClassId): List<CfirResolvedTypeRef>

    /**
     * 返回带来源信息的直接父类型边。
     *
     * 普通实现可以只提供 type ref；resolve 阶段的 graph store 会覆盖该方法，
     * 保留 extend 来源信息供后续 checker 区分“目标已有父类型”和“当前 extend 正在实现的父类型”。
     */
    fun getDirectSuperTypeEdges(ownerClassId: ClassId): List<CfirSuperTypeGraphEdge> =
        getDirectSuperTypes(ownerClassId).map { typeRef ->
            CfirSuperTypeGraphEdge(
                typeRef = typeRef,
                renderedType = typeRef.toString(),
                resolvedClassSymbol = null,
            )
        }
}

/**
 * 合并声明超类型和 extend 超类型，并按稳定 key 去重。
 */
fun mergeSuperTypes(
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
fun CfirSuperTypeGraphEdge.stableKey(): String {
    return resolvedClassSymbol?.classId?.asString() ?: renderedType
}
