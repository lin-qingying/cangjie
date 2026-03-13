package org.cangjie.cfir.resolve

import org.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangjie.cfir.diagnostics.reportOn
import org.cangjie.cfir.resolve.diagnostics.CfirResolveRuleCatalog
import org.cangjie.cfir.resolve.services.CfirResolvedImportBinding

private val RULE_IMPORTS_BINDING = CfirResolveRuleCatalog.IMPORTS_BINDING
private val RULE_IMPORTS_CONFLICT = CfirResolveRuleCatalog.IMPORTS_CONFLICT

internal class CfirImportConflictReporter(
    private val diagnosticReporter: CfirDiagnosticReporter,
) {
    fun reportUnresolvedTargets(resolvedImports: List<CfirResolvedImportBinding>) {
        resolvedImports
            .filter { it.targets.isEmpty() }
            .forEach { unresolved ->
                diagnosticReporter.reportOn(
                    source = unresolved.importDirective.source,
                    factory = CfirErrors.IMPORT_TARGET_NOT_FOUND,
                    a = RULE_IMPORTS_BINDING.id,
                    b = "unresolved import target '${unresolved.importDirective.importedFqName.asString()}' (${RULE_IMPORTS_BINDING.officialReference})",
                    context = DiagnosticContext.Default,
                )
            }
    }

    fun reportConflicts(resolvedImports: List<CfirResolvedImportBinding>) {
        resolvedImports
            .groupBy { it.effectiveName }
            .forEach { (effectiveName, sameNameBindings) ->
                if (sameNameBindings.size < 2) return@forEach
                val signatures = sameNameBindings.map { it.stableTargetSignature() }.toSet()
                if (signatures.size > 1) {
                    diagnosticReporter.reportOn(
                        source = sameNameBindings.first().importDirective.source,
                        factory = CfirErrors.IMPORT_CONFLICT,
                        a = RULE_IMPORTS_CONFLICT.id,
                        b = "conflicting imports for name '${effectiveName.asString()}' (${RULE_IMPORTS_CONFLICT.officialReference})",
                        context = DiagnosticContext.Default,
                    )
                }
            }

        resolvedImports
            .filter { it.importDirective.aliasName != null }
            .groupBy { it.importDirective.aliasName!! }
            .forEach { (aliasName, sameAliasBindings) ->
                if (sameAliasBindings.size < 2) return@forEach
                val signatures = sameAliasBindings.map { it.stableTargetSignature() }.toSet()
                if (signatures.size > 1) {
                    diagnosticReporter.reportOn(
                        source = sameAliasBindings.first().importDirective.source,
                        factory = CfirErrors.IMPORT_ALIAS_CONFLICT,
                        a = RULE_IMPORTS_CONFLICT.id,
                        b = "alias conflict for '${aliasName.asString()}' (${RULE_IMPORTS_CONFLICT.officialReference})",
                        context = DiagnosticContext.Default,
                    )
                }
            }
    }
}
