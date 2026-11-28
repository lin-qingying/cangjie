package org.cangnova.cangjie.cfir.analysis.collectors.components

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.analysis.checkers.MppCheckerKind
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
        mppCheckerKind: MppCheckerKind,
    ): DiagnosticCollectorComponents {
        val regularComponents = buildList {
            add(DeclarationCheckersDiagnosticComponent(session, reporter, mppCheckerKind))
            add(ExpressionCheckersDiagnosticComponent(session, reporter, mppCheckerKind))
            add(TypeCheckersDiagnosticComponent(session, reporter, mppCheckerKind))
            if (mppCheckerKind == MppCheckerKind.Common) {
                add(MacroConstructionDiagnosticCollectorComponent(session, reporter))
                add(ErrorNodeDiagnosticCollectorComponent(session, reporter))
                add(LanguageVersionSettingsDiagnosticComponent(session, reporter))
            } else {
                add(CfirCjmpBinaryDiagnosticsCollectorComponent(session, reporter))
            }
        }.toTypedArray()
        val postSemaComponents = if (mppCheckerKind == MppCheckerKind.Common) arrayOf<AbstractDiagnosticCollectorComponent>(
            ControlFlowAnalysisDiagnosticComponent(session, reporter),
            CfirChirArithmeticDiagnosticCollectorComponent(session, reporter),
        ) else emptyArray()
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
        mppCheckerKind: MppCheckerKind,
        ): CliDiagnosticsCollector {
        return CliDiagnosticsCollector(session, scopeSession) { reporter ->
            createAllDiagnosticComponents(session, reporter, mppCheckerKind)
        }
    }
}
