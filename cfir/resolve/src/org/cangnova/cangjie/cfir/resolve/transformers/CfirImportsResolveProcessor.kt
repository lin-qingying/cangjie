package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.importTracker
import org.cangnova.cangjie.cfir.reportImportDirectives
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.replaceResolvePhase
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.resolve.CfirDiagnosticReporter
import org.cangnova.cangjie.cfir.resolve.CfirImportBindingResolver
import org.cangnova.cangjie.cfir.resolve.CfirImportConflictReporter
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.importBindingStoreOrNull

/**
 * IMPORTS 阶段处理器。
 * 它把 `IMPORTS` 阶段与对应的 transformer 绑定起来，驱动整条 import 解析流程。
 */
internal class CfirImportResolveProcessor(
    @Suppress("unused")
    private val diagnosticReporter: CfirDiagnosticReporter,
    session: CfirSession,
    scopeSession: ScopeSession,
) : CfirTransformerBasedResolveProcessor(
    session = session,
    scopeSession = scopeSession,
    phase = CfirResolvePhase.IMPORTS,
) {
    override val transformer: CfirImportResolveTransformer =
        CfirImportResolveTransformer(session, diagnosticReporter)
}

/** 与 [CfirImportResolveProcessor] 完全等价的别名。 */
internal typealias CfirImportsResolveProcessor = CfirImportResolveProcessor

/**
 * IMPORTS 阶段的树变换器。
 * 它只处理文件级 import 绑定，并在完成后推进 resolve phase。
 */
internal class CfirImportResolveTransformer(
    override val session: CfirSession,
    @Suppress("unused")
    private val diagnosticReporter: CfirDiagnosticReporter,
) : CfirAbstractTreeTransformer<Nothing?>(CfirResolvePhase.IMPORTS) {
    override fun <E : CfirElement> transformElement(element: E, data: Nothing?): E {
        if (element is CfirDeclaration) {
            @Suppress("UNCHECKED_CAST")
            return transformDeclaration(element, data) as E
        }
        return super.transformElement(element, data)
    }

    override fun transformDeclaration(declaration: CfirDeclaration, data: Nothing?): CfirDeclaration {
        if (declaration.resolvePhase < CfirResolvePhase.RAW_CFIR || declaration.resolvePhase >= CfirResolvePhase.IMPORTS) {
            return declaration
        }

        declaration.transformChildren(this, data)
        if (declaration is CfirFile) {
            val store = session.importBindingStoreOrNull
            if (store != null) {
                val bindingResolver = CfirImportBindingResolver(session)
                val conflictReporter = CfirImportConflictReporter(diagnosticReporter)
                val resolvedImports = declaration.imports.map { bindingResolver.resolveImportBinding(it) }
                conflictReporter.reportUnresolvedTargets(resolvedImports)
                conflictReporter.reportConflicts(resolvedImports)
                store.record(declaration, resolvedImports)
                val filePath = declaration.sourceFile?.path
                if (filePath != null) {
                    session.importTracker?.let { tracker ->
                        for (resolvedImport in resolvedImports) {
                            tracker.reportImportDirectives(
                                filePath,
                                resolvedImport.importDirective.importedFqName?.asString(),
                            )
                        }
                    }
                }
            }
        }

        declaration.replaceResolvePhase(CfirResolvePhase.IMPORTS)
        return declaration
    }
}
