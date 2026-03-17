package org.cangnova.cangjie.cfir.analysis.collectors

import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.diagnostics.PendingDiagnosticReporter
import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * 璇婃柇鏀堕泦缂栨帓鍣紝鍒涘缓 visitor 骞堕┍鍔ㄩ亶鍘嗐€? *
 * 瀵归綈 K2 `AbstractDiagnosticCollector`銆? */
abstract class CfirAbstractDiagnosticCollector(
    val session: CfirSession,
    protected val createComponents: (PendingDiagnosticReporter) -> CfirDiagnosticCollectorComponents,
) {
    fun collectDiagnostics(cfirDeclaration: CfirDeclaration, reporter: PendingDiagnosticReporter) {
        val components = createComponents(reporter)
        val visitor = createVisitor(components, reporter)
        visitor.checkSettings()
        cfirDeclaration.accept(visitor, null)
    }

    protected abstract fun createVisitor(
        components: CfirDiagnosticCollectorComponents,
        reporter: PendingDiagnosticReporter,
    ): CfirCheckerRunningDiagnosticCollectorVisitor
}

