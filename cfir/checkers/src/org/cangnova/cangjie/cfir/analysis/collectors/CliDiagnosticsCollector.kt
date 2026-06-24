package org.cangnova.cangjie.cfir.analysis.collectors

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.analysis.checkers.context.MutableCheckerContext
import org.cangnova.cangjie.cfir.diagnostics.PendingDiagnosticReporter
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculatorForFullBodyResolve
import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * CLI 前端使用的诊断收集器实现。
 *
 * @param session 当前 CLI 诊断流程所属的 CFIR session。
 * @param scopeSession 当前 CLI 诊断流程复用的作用域缓存。
 * @param createComponents 根据 pending reporter 创建诊断组件集合的工厂。
 */
class CliDiagnosticsCollector(
    session: CfirSession,
    scopeSession: ScopeSession,
    createComponents: (PendingDiagnosticReporter) -> DiagnosticCollectorComponents,
) : AbstractDiagnosticCollector(session, scopeSession, createComponents) {
    /** 创建带 CLI 默认返回类型计算器的 checker visitor。 */
    override fun createVisitor(
        components: DiagnosticCollectorComponents,
        reporter: PendingDiagnosticReporter,
    ): CheckerRunningDiagnosticCollectorVisitor {
        return CheckerRunningDiagnosticCollectorVisitor(
            MutableCheckerContext(
                sessionHolder = this,
                returnTypeCalculator = ReturnTypeCalculatorForFullBodyResolve.Default,
                containingFileSymbol = null,
                reporter = reporter,
            ),
            components,
        )
    }
}
