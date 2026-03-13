package org.cangjie.cfir.resolve

import org.cangjie.cfir.common.CfirSourceElement
import org.cangjie.cfir.declarations.CfirClass
import org.cangjie.cfir.declarations.CfirClassKind
import org.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangjie.cfir.diagnostics.reportOn
import org.cangjie.cfir.resolve.diagnostics.CfirResolveRuleCatalog
import org.cangjie.cfir.resolve.services.CfirSuperTypeGraphEdge
import org.cangjie.cfir.symbols.CfirClassSymbol

private val RULE_SUPER_TYPES_DUPLICATE = CfirResolveRuleCatalog.SUPER_TYPES_DUPLICATE_INTERFACE
private val RULE_SUPER_TYPES_SELF_REFERENCE = CfirResolveRuleCatalog.SUPER_TYPES_SELF_REFERENCE

internal data class CfirSuperTypeCheckResult(
    val classLikeSupers: List<CfirClass>,
    val graphEdges: List<CfirSuperTypeGraphEdge>,
)

internal class CfirSuperTypeChecker(
    private val diagnosticReporter: CfirDiagnosticReporter,
    private val resolver: CfirTypeRefResolver,
) {
    fun collectAndCheck(target: CfirClass): CfirSuperTypeCheckResult {
        val seen = linkedSetOf<String>()
        val seenResolvedInterfaceSymbols = linkedSetOf<CfirClassSymbol>()
        val classLikeSupers = mutableListOf<CfirClass>()
        val graphEdges = mutableListOf<CfirSuperTypeGraphEdge>()

        for (superTypeRef in target.superTypeRefs) {
            val key = superTypeRef.renderStableKey()
            val resolvedClass = resolver.resolveClass(superTypeRef)
            graphEdges += CfirSuperTypeGraphEdge(
                renderedType = key,
                resolvedClassSymbol = resolvedClass?.symbol,
            )

            if (!seen.add(key)) {
                reportDuplicate(target, superTypeRef.source, key)
            }
            if (resolvedClass?.classKind == CfirClassKind.INTERFACE && !seenResolvedInterfaceSymbols.add(resolvedClass.symbol)) {
                reportDuplicate(target, superTypeRef.source, key)
            }
            if (key == target.name.asString()) {
                diagnosticReporter.reportOn(
                    source = superTypeRef.source,
                    factory = CfirErrors.SUPER_TYPES_SELF_REFERENCE,
                    a = RULE_SUPER_TYPES_SELF_REFERENCE.id,
                    b = "type '${target.name}' cannot inherit from itself (${RULE_SUPER_TYPES_SELF_REFERENCE.officialReference})",
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

    private fun reportDuplicate(target: CfirClass, source: CfirSourceElement?, key: String) {
        diagnosticReporter.reportOn(
            source = source,
            factory = CfirErrors.SUPER_TYPES_DUPLICATE,
            a = RULE_SUPER_TYPES_DUPLICATE.id,
            b = "duplicate super type '$key' for type '${target.name}' (${RULE_SUPER_TYPES_DUPLICATE.officialReference})",
            context = DiagnosticContext.Default,
        )
    }
}
