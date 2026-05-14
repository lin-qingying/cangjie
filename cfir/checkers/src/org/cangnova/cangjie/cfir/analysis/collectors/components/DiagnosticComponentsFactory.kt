package org.cangnova.cangjie.cfir.analysis.collectors.components

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.DeclarationCheckersDiagnosticComponent
import org.cangnova.cangjie.cfir.analysis.checkers.expression.ExpressionCheckersDiagnosticComponent
import org.cangnova.cangjie.cfir.analysis.checkers.type.TypeCheckersDiagnosticComponent
import org.cangnova.cangjie.cfir.analysis.collectors.CliDiagnosticsCollector
import org.cangnova.cangjie.cfir.analysis.collectors.DiagnosticCollectorComponents
import org.cangnova.cangjie.cfir.diagnostics.PendingDiagnosticReporter
import org.cangnova.cangjie.cfir.session.CfirSession

object DiagnosticComponentsFactory {
    private fun createAllDiagnosticComponents(
        session: CfirSession,
        reporter: PendingDiagnosticReporter,
    ): DiagnosticCollectorComponents {
        val regularComponents = buildList {
            add(DeclarationCheckersDiagnosticComponent(session, reporter))
            add(ExpressionCheckersDiagnosticComponent(session, reporter))
            add(TypeCheckersDiagnosticComponent(session, reporter))
//            add(ControlFlowAnalysisDiagnosticComponent(session, reporter))
            add(MacroConstructionDiagnosticCollectorComponent(session, reporter))
            add(ErrorNodeDiagnosticCollectorComponent(session, reporter))
//            add(LanguageVersionSettingsDiagnosticComponent(session, reporter))
        }.toTypedArray()
        return DiagnosticCollectorComponents(
            regularComponents,
            ReportCommitterDiagnosticComponent(session, reporter)
        )
    }

    fun create(
        session: CfirSession,
        scopeSession: ScopeSession,

        ): CliDiagnosticsCollector {
        return CliDiagnosticsCollector(session, scopeSession) { reporter ->
            createAllDiagnosticComponents(session, reporter)
        }
    }
}
