package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.scopes.CfirScopeSession
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.importBindingStoreOrNull

internal class CfirImportResolveProcessor(
    private val diagnosticReporter: CfirDiagnosticReporter,
    session: CfirSession,
    scopeSession: CfirScopeSession,
) : CfirTransformerBasedResolveProcessor(
    session = session,
    scopeSession = scopeSession,
    phase = CfirResolvePhase.IMPORTS,
) {
    override val transformer: CfirImportResolveTransformer =
        CfirImportResolveTransformer(session, diagnosticReporter)
}

typealias CfirImportsResolveProcessor = CfirImportResolveProcessor

internal class CfirImportResolveTransformer(
    override val session: CfirSession,
    private val diagnosticReporter: CfirDiagnosticReporter,
) : CfirAbstractTreeTransformer<Nothing?>(CfirResolvePhase.IMPORTS) {
    override fun transformDeclaration(declaration: CfirDeclaration, data: Nothing?): CfirDeclaration {
        if (declaration.resolvePhase < CfirResolvePhase.RAW_CFIR || declaration.resolvePhase >= CfirResolvePhase.IMPORTS) {
            return declaration
        }

        if (declaration is CfirFile) {
            val store = session.importBindingStoreOrNull
            if (store != null) {
                val bindingResolver = CfirImportBindingResolver(session)
                val conflictReporter = CfirImportConflictReporter(diagnosticReporter)

                val resolvedImports = declaration.imports.map { bindingResolver.resolveImportBinding(it) }
                conflictReporter.reportUnresolvedTargets(resolvedImports)
                conflictReporter.reportConflicts(resolvedImports)
                store.record(declaration, resolvedImports)
            }
        }

        declaration.resolvePhase = CfirResolvePhase.IMPORTS
        return declaration
    }
}
