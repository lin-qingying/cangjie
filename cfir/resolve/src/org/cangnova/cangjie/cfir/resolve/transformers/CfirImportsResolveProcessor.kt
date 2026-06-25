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
    /** IMPORTS 阶段使用的诊断报告器。 */
    private val diagnosticReporter: CfirDiagnosticReporter,
    session: CfirSession,
    scopeSession: ScopeSession,
) : CfirTransformerBasedResolveProcessor(
    session = session,
    scopeSession = scopeSession,
    phase = CfirResolvePhase.IMPORTS,
) {
    /** IMPORTS 阶段使用的 transformer。 */
    override val transformer: CfirImportResolveTransformer =
        CfirImportResolveTransformer(session, diagnosticReporter)
}

/** 与 [CfirImportResolveProcessor] 完全等价的别名。 */
internal typealias CfirImportsResolveProcessor = CfirImportResolveProcessor

/**
 * IMPORTS 阶段的树变换器。
 * 它只处理文件级 import 绑定，并在完成后推进 resolve phase。
 */
class CfirImportResolveTransformer(
    /** 当前 CFIR session。 */
    override val session: CfirSession,
    @Suppress("unused")
    /** IMPORTS 阶段使用的诊断报告器。 */
    private val diagnosticReporter: CfirDiagnosticReporter,
) : CfirAbstractTreeTransformer<Nothing?>(CfirResolvePhase.IMPORTS) {
    /** 对非声明元素继续递归转换；声明元素转交 [transformDeclaration] 控制阶段推进。 */
    override fun <E : CfirElement> transformElement(element: E, data: Nothing?): E {
        if (element is CfirDeclaration) {
            @Suppress("UNCHECKED_CAST")
            return transformDeclaration(element, data) as E
        }
        return super.transformElement(element, data)
    }

    /** 解析文件 import binding，并将声明推进到 IMPORTS 阶段。 */
    override fun transformDeclaration(declaration: CfirDeclaration, data: Nothing?): CfirDeclaration {
        if (declaration is CfirFile && declaration.resolvePhase >= CfirResolvePhase.IMPORTS) {
            recordImportBindingsIfNeeded(declaration)
            return declaration
        }
        if (declaration.resolvePhase < CfirResolvePhase.RAW_CFIR || declaration.resolvePhase >= CfirResolvePhase.IMPORTS) {
            return declaration
        }

        declaration.transformChildren(this, data)
        if (declaration is CfirFile) {
            recordImportBindingsIfNeeded(declaration)
        }

        declaration.replaceResolvePhase(CfirResolvePhase.IMPORTS)
        return declaration
    }

    /**
     * Low-level resolve 可以在 original/dangling CFIR 文件副本之间复用已推进的 phase。
     * IMPORTS phase 对应的 session 级 binding store 必须按当前文件副本补齐，否则后续
     * body resolve 无法通过同一文件对象读取 import 绑定。
     */
    private fun recordImportBindingsIfNeeded(file: CfirFile) {
        val store = session.importBindingStoreOrNull ?: return
        if (store.getBindings(file) != null) return

        val bindingResolver = CfirImportBindingResolver(session)
        val conflictReporter = CfirImportConflictReporter(diagnosticReporter)
        val resolvedImports = file.imports.map { bindingResolver.resolveImportBinding(it) }
        conflictReporter.reportUnresolvedTargets(resolvedImports)
        conflictReporter.reportConflicts(resolvedImports)
        store.record(file, resolvedImports)
        val filePath = file.sourceFile?.path ?: return
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
