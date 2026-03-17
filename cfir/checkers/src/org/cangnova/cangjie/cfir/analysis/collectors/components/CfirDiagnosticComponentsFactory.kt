package org.cangnova.cangjie.cfir.analysis.collectors.components

import org.cangnova.cangjie.cfir.analysis.checkers.CheckerDispatchKind
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirDeclarationCheckersDiagnosticComponent
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirExpressionCheckersDiagnosticComponent
import org.cangnova.cangjie.cfir.analysis.collectors.CfirCliDiagnosticsCollector
import org.cangnova.cangjie.cfir.analysis.collectors.CfirDiagnosticCollectorComponents
import org.cangnova.cangjie.cfir.diagnostics.PendingDiagnosticReporter
import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * 璇婃柇缁勪欢宸ュ巶锛岀粍瑁呮墍鏈夎瘖鏂敹闆嗙粍浠跺苟鍒涘缓 Collector銆? *
 * 瀵归綈 K2 `DiagnosticComponentsFactory`銆? */
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

