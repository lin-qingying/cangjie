package org.cangnova.cangjie.cfir.analysis.collectors.components

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.diagnostics.PendingDiagnosticReporter
import org.cangnova.cangjie.cfir.resolve.dfa.controlFlowGraph
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.source.AbstractCjSourceElement

/**
 * 函数 CFG 的控制流诊断组件。
 *
 * 它在函数子树的 Sema checker 已完成后消费完整 CFG。这样 matrix 覆盖诊断与常量分支
 * 可达性各自保有独立 owner：前者属于表达式 checker，后者对位官方 CHIR
 * `ConstAnalysis` 加 `UnreachableBranchCheck` 的后端控制流阶段。
 */
class ControlFlowAnalysisDiagnosticComponent(
    session: CfirSession,
    reporter: PendingDiagnosticReporter,
) : AbstractDiagnosticCollectorComponent(session, reporter) {
    /** 退出函数后才能得到完整且冻结的 CFG。 */
    override fun onDeclarationExit(declaration: CfirDeclaration, data: CheckerContext) {
        val function = declaration as? CfirFunction ?: return
        val graph = function.controlFlowGraphReference?.controlFlowGraph ?: return
        // 与官方 CHIR UnreachableBranchCheck 相同，CFA 只消费 CFG 常量分支目标；
        // Sema pattern legality 诊断不参与这一后端控制流判定。
        val unreachablePatterns = CfirControlFlowConstAnalysis().collectUnreachablePatterns(graph)
        for (pattern in unreachablePatterns) {
            reportOn(
                source = pattern.source as? AbstractCjSourceElement,
                factory = CfirErrors.UNREACHABLE_PATTERN,
                context = data,
            )
        }
    }
}
