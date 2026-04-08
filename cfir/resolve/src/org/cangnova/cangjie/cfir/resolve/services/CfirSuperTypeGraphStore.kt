package org.cangnova.cangjie.cfir.resolve.services

import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.resolve.providers.CfirDirectSupertypeProvider
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.name.ClassId

data class CfirSuperTypeGraphEdge(
    val typeRef: CfirResolvedTypeRef,
    val renderedType: String,
    val resolvedClassSymbol: CfirClassLikeSymbol<*>?,
)

data class CfirSuperTypeGraphNode(
    val ownerClassId: ClassId,
    val declaredSuperTypes: List<CfirSuperTypeGraphEdge>,
    val extendedSuperTypes: List<CfirSuperTypeGraphEdge>,
) {
    val superTypes: List<CfirSuperTypeGraphEdge>
        get() = mergeSuperTypes(declaredSuperTypes, extendedSuperTypes)
}

class CfirSuperTypeGraphStore : CfirSessionComponent, CfirDirectSupertypeProvider {
    private val declaredSuperTypesByOwner = mutableMapOf<ClassId, List<CfirSuperTypeGraphEdge>>()
    private val extendedSuperTypesByOwner = mutableMapOf<ClassId, List<CfirSuperTypeGraphEdge>>()

    fun recordDeclared(owner: CfirClassLikeDeclaration, superTypes: List<CfirSuperTypeGraphEdge>) {
        val ownerSymbol = owner.symbol as? CfirClassLikeSymbol<*> ?: return
        declaredSuperTypesByOwner[ownerSymbol.classId] = superTypes.distinctBy(CfirSuperTypeGraphEdge::stableKey)
    }

    fun recordExtended(ownerClassId: ClassId, superTypes: List<CfirSuperTypeGraphEdge>) {
        val existing = extendedSuperTypesByOwner[ownerClassId].orEmpty()
        extendedSuperTypesByOwner[ownerClassId] =
            mergeSuperTypes(existing, superTypes).distinctBy(CfirSuperTypeGraphEdge::stableKey)
    }

    fun clearExtended() {
        extendedSuperTypesByOwner.clear()
    }

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

    fun getDirectSuperTypeEdges(ownerClassId: ClassId): List<CfirSuperTypeGraphEdge> =
        getNode(ownerClassId)?.superTypes.orEmpty()

    override fun getDirectSuperTypes(ownerClassId: ClassId): List<CfirResolvedTypeRef> =
        getDirectSuperTypeEdges(ownerClassId).map(CfirSuperTypeGraphEdge::typeRef)
}

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

private fun CfirSuperTypeGraphEdge.stableKey(): String {
    return resolvedClassSymbol?.classId?.asString() ?: renderedType
}
