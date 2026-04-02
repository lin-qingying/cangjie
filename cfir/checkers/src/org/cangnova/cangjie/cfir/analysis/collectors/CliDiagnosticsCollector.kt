package org.cangnova.cangjie.cfir.analysis.collectors

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.analysis.checkers.context.MutableCheckerContext
import org.cangnova.cangjie.cfir.diagnostics.PendingDiagnosticReporter
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculatorForFullBodyResolve
import org.cangnova.cangjie.cfir.session.CfirSession

class CliDiagnosticsCollector(
    session: CfirSession,
    scopeSession: ScopeSession,
    createComponents: (PendingDiagnosticReporter) -> DiagnosticCollectorComponents,
) : AbstractDiagnosticCollector(session, scopeSession, createComponents) {
    override fun createVisitor(
        components: DiagnosticCollectorComponents,
        reporter: PendingDiagnosticReporter,
    ): CheckerRunningDiagnosticCollectorVisitor {
        return CheckerRunningDiagnosticCollectorVisitor(
            MutableCheckerContext(
                sessionHolder = this,
                returnTypeCalculator = ReturnTypeCalculatorForFullBodyResolve.Default,
                containingFileSymbol = null,
                reporter = reporter,
            ),
            components,
        )
    }
}
