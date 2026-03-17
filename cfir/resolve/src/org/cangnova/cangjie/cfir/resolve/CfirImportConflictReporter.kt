package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.resolve.services.CfirResolvedImportBinding

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
                    a = unresolved.importDirective.importedFqName,
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
                        a = effectiveName,
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
                        a = aliasName,
                        context = DiagnosticContext.Default,
                    )
                }
            }
    }
}

