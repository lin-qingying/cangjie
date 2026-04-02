package org.cangnova.cangjie.cfir.analysis.collectors

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.SessionAndScopeSessionHolder
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.diagnostics.PendingDiagnosticReporter
import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * Coordinates diagnostic collection by creating a visitor and running it on declarations.
 * The structure follows Kotlin FIR's AbstractDiagnosticCollector while keeping the reporter
 * threaded through the context for the current CFIR pipeline.
 */
abstract class AbstractDiagnosticCollector(
    override val session: CfirSession,
    override val scopeSession: ScopeSession = ScopeSession(),
    protected val createComponents: (PendingDiagnosticReporter) -> DiagnosticCollectorComponents,
) : SessionAndScopeSessionHolder {
    fun collectDiagnostics(cfirDeclaration: CfirDeclaration, reporter: PendingDiagnosticReporter) {
        val components = createComponents(reporter)
        val visitor = createVisitor(components, reporter)
        visitor.checkSettings()
        cfirDeclaration.accept(visitor, null)
    }

    fun collectDiagnosticsInSettings(reporter: PendingDiagnosticReporter) {
        val visitor = createVisitor(createComponents(reporter), reporter)
        visitor.checkSettings()
    }

    protected abstract fun createVisitor(
        components: DiagnosticCollectorComponents,
        reporter: PendingDiagnosticReporter,
    ): CheckerRunningDiagnosticCollectorVisitor
}
