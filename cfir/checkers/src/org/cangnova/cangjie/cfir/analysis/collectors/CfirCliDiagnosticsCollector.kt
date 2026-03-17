package org.cangnova.cangjie.cfir.analysis.collectors

import org.cangnova.cangjie.cfir.analysis.checkers.context.MutableCheckerContext
import org.cangnova.cangjie.cfir.diagnostics.PendingDiagnosticReporter
import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * CLI / 娴嬭瘯鐜鐨勮瘖鏂敹闆嗗櫒銆? *
 * 瀵归綈 K2 `CliDiagnosticsCollector`銆? */
class CfirCliDiagnosticsCollector(
    session: CfirSession,
    createComponents: (PendingDiagnosticReporter) -> CfirDiagnosticCollectorComponents,
) : CfirAbstractDiagnosticCollector(session, createComponents) {

    override fun createVisitor(
        components: CfirDiagnosticCollectorComponents,
        reporter: PendingDiagnosticReporter,
    ): CfirCheckerRunningDiagnosticCollectorVisitor {
        return CfirCheckerRunningDiagnosticCollectorVisitor(
            MutableCheckerContext(
                file = null,
                session = session,
                reporter = reporter,
            ),
            components,
        )
    }
}

