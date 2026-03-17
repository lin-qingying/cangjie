package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassKind
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.resolve.services.CfirSuperTypeGraphEdge
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.name.Name

internal data class CfirSuperTypeCheckResult(
    val classLikeSupers: List<CfirClass>,
    val graphEdges: List<CfirSuperTypeGraphEdge>,
)

internal class CfirSuperTypeChecker(
    private val diagnosticReporter: CfirDiagnosticReporter,
    private val resolver: CfirTypeResolver,
) {
    fun collectAndCheck(target: CfirClass): CfirSuperTypeCheckResult {
        val seen = linkedMapOf<String, CjSourceElement?>()
        val seenResolvedInterfaceSymbols = linkedSetOf<CfirClassSymbol>()
        val firstSourceByInterfaceSymbol = linkedMapOf<CfirClassSymbol, CjSourceElement?>()
        val classLikeSupers = mutableListOf<CfirClass>()
        val graphEdges = mutableListOf<CfirSuperTypeGraphEdge>()

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
                reportDuplicate(target, firstSourceByKey, key, resolvedClass?.name)
            }
            val interfaceSymbol = resolvedClass?.takeIf { it.classKind == CfirClassKind.INTERFACE }?.symbol as? CfirClassSymbol
            if (interfaceSymbol != null) {
                firstSourceByInterfaceSymbol.putIfAbsent(interfaceSymbol, superTypeRef.source)
                val duplicateBySymbol = !seenResolvedInterfaceSymbols.add(interfaceSymbol)
                if (duplicateBySymbol && !duplicateByKey) {
                    reportDuplicate(
                        target,
                        firstSourceByInterfaceSymbol[interfaceSymbol] ?: superTypeRef.source,
                        key,
                        resolvedClass.name,
                    )
                }
            }
            val approximateTypeName = key.toApproxName()
            if (approximateTypeName == target.name || resolvedClass?.name == target.name) {
                diagnosticReporter.reportOn(
                    source = superTypeRef.source,
                    factory = CfirErrors.SUPER_TYPES_SELF_REFERENCE,
                    a = target.name,
                    context = DiagnosticContext.Default,
                )
            }
            resolvedClass?.let { classLikeSupers += it }
        }

        return CfirSuperTypeCheckResult(
            classLikeSupers = classLikeSupers,
            graphEdges = graphEdges,
        )
    }

    private fun reportDuplicate(target: CfirClass, source: CjSourceElement?, key: String, resolvedName: Name?) {
        diagnosticReporter.reportOn(
            source = source,
            factory = CfirErrors.SUPER_TYPES_DUPLICATE,
            a = resolvedName ?: key.toApproxName(),
            context = DiagnosticContext.Default,
        )
    }
}

private fun String.toApproxName(): Name {
    val raw = substringAfterLast('.').substringBefore('<')
    return Name.identifierIfValid(raw) ?: Name.ERROR_NAME
}

