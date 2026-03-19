package org.cangnova.cangjie.cfir.analysis.collectors.components

import org.cangnova.cangjie.cfir.analysis.checkers.CheckerDispatchKind
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirDeclarationCheckersDiagnosticComponent
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirExpressionCheckersDiagnosticComponent
import org.cangnova.cangjie.cfir.analysis.collectors.CfirCliDiagnosticsCollector
import org.cangnova.cangjie.cfir.analysis.collectors.CfirDiagnosticCollectorComponents
import org.cangnova.cangjie.cfir.diagnostics.PendingDiagnosticReporter
import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * 诊断组件工厂，负责组装所有诊断收集组件并创建 collector。
 * 对齐 K2 `DiagnosticComponentsFactory`。
 */
object CfirDiagnosticComponentsFactory {

    private fun createAllComponents(
        session: CfirSession,
        reporter: PendingDiagnosticReporter,
        dispatchKind: CheckerDispatchKind,
    ): CfirDiagnosticCollectorComponents {
        val regularComponents = buildList {
            add(CfirErrorNodeDiagnosticCollectorComponent(session, reporter))
            add(CfirDeclarationCheckersDiagnosticComponent(session, reporter, dispatchKind))
            add(CfirExpressionCheckersDiagnosticComponent(session, reporter, dispatchKind))
        }.toTypedArray()
        return CfirDiagnosticCollectorComponents(
            regularComponents,
            CfirReportCommitterDiagnosticComponent(session, reporter),
        )
    }

    fun create(session: CfirSession, dispatchKind: CheckerDispatchKind): CfirCliDiagnosticsCollector {
        return CfirCliDiagnosticsCollector(session) { reporter ->
            createAllComponents(session, reporter, dispatchKind)
        }
    }
}

