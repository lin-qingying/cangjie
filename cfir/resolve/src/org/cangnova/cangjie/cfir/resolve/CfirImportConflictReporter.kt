package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.resolve.services.CfirResolvedImportBinding
import org.cangnova.cangjie.name.Name

internal data class CfirImportConflicts(
    val unresolvedTargets: List<CfirResolvedImportBinding>,
    val conflictingNames: Map<Name, List<CfirResolvedImportBinding>>,
    val conflictingAliases: Map<Name, List<CfirResolvedImportBinding>>,
)

internal class CfirImportConflictReporter(
    @Suppress("unused")
    private val diagnosticReporter: CfirDiagnosticReporter? = null,
) {
    fun analyze(resolvedImports: List<CfirResolvedImportBinding>): CfirImportConflicts {
        val unresolvedTargets = resolvedImports.filter { it.targets.isEmpty() }
        val conflictingNames = resolvedImports
            .groupBy { it.effectiveName }
            .filterValues { sameNameBindings ->
                sameNameBindings.size >= 2 &&
                    sameNameBindings.map { it.stableTargetSignature() }.toSet().size > 1
            }
        val conflictingAliases = resolvedImports
            .filter { it.importDirective.aliasName != null }
            .groupBy { it.importDirective.aliasName!! }
            .filterValues { sameAliasBindings ->
                sameAliasBindings.size >= 2 &&
                    sameAliasBindings.map { it.stableTargetSignature() }.toSet().size > 1
            }
        return CfirImportConflicts(
            unresolvedTargets = unresolvedTargets,
            conflictingNames = conflictingNames,
            conflictingAliases = conflictingAliases,
        )
    }

    fun reportUnresolvedTargets(resolvedImports: List<CfirResolvedImportBinding>) = analyze(resolvedImports).unresolvedTargets

    fun reportConflicts(resolvedImports: List<CfirResolvedImportBinding>) = analyze(resolvedImports)
}
