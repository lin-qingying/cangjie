package org.cangnova.cangjie.cfir.analysis.collectors

import org.cangnova.cangjie.cfir.analysis.checkers.context.MutableCheckerContext
import org.cangnova.cangjie.cfir.diagnostics.PendingDiagnosticReporter
import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * CLI / 测试环境下的诊断收集器。
 * 对齐 K2 `CliDiagnosticsCollector`。
 */
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

