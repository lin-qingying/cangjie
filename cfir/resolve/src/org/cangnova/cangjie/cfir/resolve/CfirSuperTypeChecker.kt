package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.resolve.services.CfirSuperTypeGraphEdge
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.name.Name

internal data class CfirSuperTypeCheckResult(
    val classLikeSupers: List<CfirClassLikeDeclaration>,
    val graphEdges: List<CfirSuperTypeGraphEdge>,
    val duplicateTypes: List<Name>,
    val selfReferencedTypes: List<Name>,
)

internal class CfirSuperTypeChecker(
    private val resolver: CfirTypeResolver,
) {
    fun collectAndCheck(target: CfirClass): CfirSuperTypeCheckResult {
        val seen = linkedMapOf<String, CjSourceElement?>()
        val seenResolvedInterfaceSymbols = linkedSetOf<CfirClassSymbol>()
        val firstSourceByInterfaceSymbol = linkedMapOf<CfirClassSymbol, CjSourceElement?>()
        val classLikeSupers = mutableListOf<CfirClassLikeDeclaration>()
        val graphEdges = mutableListOf<CfirSuperTypeGraphEdge>()
        val duplicateTypes = linkedSetOf<Name>()
        val selfReferencedTypes = linkedSetOf<Name>()

        for (superTypeRef in target.superTypeRefs) {
            val key = superTypeRef.renderStableKey()
            val resolvedClass = resolver.resolveClass(superTypeRef)
            graphEdges += CfirSuperTypeGraphEdge(
                renderedType = key,
                resolvedClassSymbol = resolvedClass?.symbol as? CfirClassSymbol,
            )

            val firstSourceByKey = seen.putIfAbsent(key, superTypeRef.source)
            val duplicateByKey = firstSourceByKey != null
            if (duplicateByKey) {
                duplicateTypes += resolvedClass?.classLikeNameOrNull() ?: key.toApproxName()
            }

            val interfaceSymbol = (resolvedClass as? CfirInterface)?.symbol as? CfirClassSymbol
            if (interfaceSymbol != null) {
                firstSourceByInterfaceSymbol.putIfAbsent(interfaceSymbol, superTypeRef.source)
                val duplicateBySymbol = !seenResolvedInterfaceSymbols.add(interfaceSymbol)
                if (duplicateBySymbol && !duplicateByKey) {
                    duplicateTypes += resolvedClass.classLikeNameOrNull() ?: key.toApproxName()
                }
            }

            val approximateTypeName = key.toApproxName()
            if (approximateTypeName == target.name || resolvedClass?.classLikeNameOrNull() == target.name) {
                selfReferencedTypes += target.name
            }
            resolvedClass?.let { classLikeSupers += it }
        }

        return CfirSuperTypeCheckResult(
            classLikeSupers = classLikeSupers,
            graphEdges = graphEdges,
            duplicateTypes = duplicateTypes.toList(),
            selfReferencedTypes = selfReferencedTypes.toList(),
        )
    }
}

private fun String.toApproxName(): Name {
    val raw = substringAfterLast('.').substringBefore('<')
    return Name.identifierIfValid(raw) ?: Name.ERROR_NAME
}

private fun CfirClassLikeDeclaration.classLikeNameOrNull(): Name? = when (this) {
    is CfirClass -> name
    is CfirInterface -> name
    is CfirStruct -> name
    is CfirEnum -> name
    is CfirTypeAlias -> name
}
