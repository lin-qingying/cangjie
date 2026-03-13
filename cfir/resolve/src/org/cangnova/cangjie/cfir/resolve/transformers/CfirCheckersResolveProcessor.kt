package org.cangjie.cfir.resolve.transformers

import org.cangjie.cfir.CfirElement
import org.cangjie.cfir.analysis.checkers.MppCheckerKind
import org.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangjie.cfir.analysis.checkers.context.MutableCheckerContext
import org.cangjie.cfir.analysis.checkers.declaration.CfirDeclarationCheckersDiagnosticComponent
import org.cangjie.cfir.analysis.checkers.expression.CfirExpressionCheckersDiagnosticComponent
import org.cangjie.cfir.analysis.checkers.type.CfirTypeCheckersDiagnosticComponent
import org.cangjie.cfir.analysis.nullableCheckersComponent
import org.cangjie.cfir.declarations.CfirDeclaration
import org.cangjie.cfir.declarations.CfirFile
import org.cangjie.cfir.declarations.CfirInvalidDeclaration
import org.cangjie.cfir.declarations.CfirResolvePhase
import org.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangjie.cfir.diagnostics.impl.PendingDiagnosticsReporterImpl
import org.cangjie.cfir.diagnostics.reportOn
import org.cangjie.cfir.expressions.CfirStatement
import org.cangjie.cfir.resolve.CfirDiagnosticReporter
import org.cangjie.cfir.resolve.diagnostics.CfirResolveRuleCatalog
import org.cangjie.cfir.scopes.CfirScopeSession
import org.cangjie.cfir.session.CfirSession
import org.cangjie.cfir.source.AbstractCjSourceElement
import org.cangjie.cfir.visitors.CfirVisitorVoid

private const val SYNTHETIC_INVALID_DECLARATION_PREFIX = "<synthetic>"
private val RULE_CHECKERS_FINAL_RESOLVE_DIAGNOSTICS = CfirResolveRuleCatalog.CHECKERS_FINAL_RESOLVE_DIAGNOSTICS

internal class CfirCheckersResolveProcessor(
    private val diagnosticReporter: CfirDiagnosticReporter,
    session: CfirSession,
    scopeSession: CfirScopeSession,
) : CfirTransformerBasedResolveProcessor(
    session = session,
    scopeSession = scopeSession,
    phase = CfirResolvePhase.CHECKERS,
) {
    override fun processFile(file: CfirFile) {
        runCheckers(file)
        super.processFile(file)
    }

    override val transformer: CfirCheckersResolveTransformer =
        CfirCheckersResolveTransformer(session, diagnosticReporter)

    private fun runCheckers(file: CfirFile) {
        if (session.nullableCheckersComponent == null) return
        val pendingReporter = PendingDiagnosticsReporterImpl(diagnosticReporter)
        val context = MutableCheckerContext(
            file = file,
            session = session,
            reporter = pendingReporter,
        )

        val declarationComponent = CfirDeclarationCheckersDiagnosticComponent(session, pendingReporter, MppCheckerKind.Common)
        val expressionComponent = CfirExpressionCheckersDiagnosticComponent(session, pendingReporter, MppCheckerKind.Common)
        val typeComponent = CfirTypeCheckersDiagnosticComponent(session, pendingReporter, MppCheckerKind.Common)

        file.accept(
            CfirCheckersRunnerVisitor(
                declarationComponent = declarationComponent,
                expressionComponent = expressionComponent,
                typeComponent = typeComponent,
                context = context,
            ),
        )

        val source = file.source as? AbstractCjSourceElement ?: return
        pendingReporter.checkAndCommitReportsOn(source, context, commitEverything = true)
    }
}

internal class CfirCheckersResolveTransformer(
    override val session: CfirSession,
    private val diagnosticReporter: CfirDiagnosticReporter,
) : CfirAbstractTreeTransformer<Nothing?>(CfirResolvePhase.CHECKERS) {
    override fun transformDeclaration(declaration: CfirDeclaration, data: Nothing?): CfirDeclaration {
        if (declaration.resolvePhase < CfirResolvePhase.BODY_RESOLVE || declaration.resolvePhase >= CfirResolvePhase.CHECKERS) {
            return declaration
        }

        if (
            session.nullableCheckersComponent == null &&
            declaration is CfirInvalidDeclaration &&
            !declaration.reason.startsWith(SYNTHETIC_INVALID_DECLARATION_PREFIX)
        ) {
            val source = declaration.source as? AbstractCjSourceElement
            if (source != null) {
                diagnosticReporter.reportOn(
                    source,
                    CfirErrors.INVALID_DECLARATION,
                    "${declaration.reason} (${RULE_CHECKERS_FINAL_RESOLVE_DIAGNOSTICS.officialReference})",
                    DiagnosticContext.Default,
                )
            }
        }

        declaration.resolvePhase = CfirResolvePhase.CHECKERS
        return declaration
    }
}

private class CfirCheckersRunnerVisitor(
    private val declarationComponent: CfirDeclarationCheckersDiagnosticComponent,
    private val expressionComponent: CfirExpressionCheckersDiagnosticComponent,
    private val typeComponent: CfirTypeCheckersDiagnosticComponent,
    private val context: MutableCheckerContext,
) : CfirVisitorVoid() {
    override fun visitElement(element: CfirElement) {
        context.addElement(element)
        if (element is CfirDeclaration) {
            context.addDeclaration(element)
        }
        if (element is CfirStatement) {
            context.addStatement(element)
        }

        element.accept(declarationComponent, context)
        element.accept(expressionComponent, context)
        element.accept(typeComponent, context)
        element.acceptChildren(this)

        if (element is CfirStatement) {
            context.dropStatement()
        }
        if (element is CfirDeclaration) {
            context.dropDeclaration()
        }
        context.dropElement()
    }
}
