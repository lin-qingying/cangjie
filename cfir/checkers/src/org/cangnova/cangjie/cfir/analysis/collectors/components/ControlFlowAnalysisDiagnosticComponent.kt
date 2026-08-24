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
 *
 * 官方存在两个同文案的 unreachable pattern 诊断：Sema 的 `sema_unreachable_pattern`
 * 走 `PatternUsefulness`，与错误共存；CHIR 的 `chir_unreachable_pattern` 属于后端阶段，
 * Sema 报错后整条 CHIR 流水线不再运行，因而一条都不会出现。本组件对应后者，故必须
 * 受同样的阶段门控。
 *
 * 偏差说明：官方按整个 package 门控，而 CFIR 的诊断收集以 structure element 为粒度、
 * 每次收集使用独立 reporter（见 `collectForStructureElement`），无法观察兄弟声明的
 * 错误。这里退到"当前声明子树无错误"这一可在本架构中稳定观察的范围；它比官方门控更窄，
 * 只会保留更多诊断，不会凭空抑制。
 */
class ControlFlowAnalysisDiagnosticComponent(
    session: CfirSession,
    reporter: PendingDiagnosticReporter,
) : AbstractDiagnosticCollectorComponent(session, reporter) {
    /** 退出函数后才能得到完整且冻结的 CFG。 */
    override fun onDeclarationExit(declaration: CfirDeclaration, data: CheckerContext) {
        val function = declaration as? CfirFunction ?: return
        // 声明子树的 Sema 诊断此时已全部提交；存在错误即等同官方 CHIR 阶段未启动。
        if (reporter.hasErrors) return
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
