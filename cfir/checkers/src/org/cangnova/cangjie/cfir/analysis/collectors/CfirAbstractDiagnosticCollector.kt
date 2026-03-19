package org.cangnova.cangjie.cfir.analysis.collectors

import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.diagnostics.PendingDiagnosticReporter
import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * 诊断收集编排器，负责创建 visitor 并驱动遍历。
 * 对齐 K2 `AbstractDiagnosticCollector`。
 */
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

