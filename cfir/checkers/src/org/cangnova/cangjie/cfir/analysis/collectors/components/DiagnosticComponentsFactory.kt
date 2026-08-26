package org.cangnova.cangjie.cfir.analysis.collectors.components

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.DeclarationCheckersDiagnosticComponent
import org.cangnova.cangjie.cfir.analysis.checkers.expression.ExpressionCheckersDiagnosticComponent
import org.cangnova.cangjie.cfir.analysis.checkers.type.TypeCheckersDiagnosticComponent
import org.cangnova.cangjie.cfir.analysis.collectors.CliDiagnosticsCollector
import org.cangnova.cangjie.cfir.analysis.collectors.DiagnosticCollectorComponents
import org.cangnova.cangjie.cfir.diagnostics.PendingDiagnosticReporter
import org.cangnova.cangjie.cfir.session.CfirSession

/** CLI 诊断收集器及其默认组件集合的工厂。 */
object DiagnosticComponentsFactory {
    /** 创建一次诊断收集所需的全部常规组件和提交组件。 */
    private fun createAllDiagnosticComponents(
        session: CfirSession,
        reporter: PendingDiagnosticReporter,
    ): DiagnosticCollectorComponents {
        val regularComponents = buildList {
            add(DeclarationCheckersDiagnosticComponent(session, reporter))
            add(ExpressionCheckersDiagnosticComponent(session, reporter))
            add(TypeCheckersDiagnosticComponent(session, reporter))
            add(ControlFlowAnalysisDiagnosticComponent(session, reporter))
            add(MacroConstructionDiagnosticCollectorComponent(session, reporter))
            add(ErrorNodeDiagnosticCollectorComponent(session, reporter))
//            add(LanguageVersionSettingsDiagnosticComponent(session, reporter))
        }.toTypedArray()
        val postSemaComponents = arrayOf<AbstractDiagnosticCollectorComponent>(
            CfirChirArithmeticDiagnosticCollectorComponent(session, reporter),
        )
        return DiagnosticCollectorComponents(
            regularComponents = regularComponents,
            postSemaComponents = postSemaComponents,
            reportCommitter = ReportCommitterDiagnosticComponent(session, reporter),
        )
    }

    /** 为指定 session/scopeSession 创建 CLI 诊断收集器。 */
    fun create(
        session: CfirSession,
        scopeSession: ScopeSession,

        ): CliDiagnosticsCollector {
        return CliDiagnosticsCollector(session, scopeSession) { reporter ->
            createAllDiagnosticComponents(session, reporter)
        }
    }
}
