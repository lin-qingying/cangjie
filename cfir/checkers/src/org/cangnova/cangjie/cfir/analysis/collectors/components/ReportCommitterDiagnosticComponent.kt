package org.cangnova.cangjie.cfir.analysis.collectors.components

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.diagnostics.PendingDiagnosticReporter
import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * 在每个元素访问后，将 pending 诊断提交到最终的 diagnostic collector。
 * 对齐 K2 `ReportCommitterDiagnosticComponent`。
 * 执行时机：
 * - `visitElement()`：常规组件检查完成后，提交当前元素上的 pending 诊断
 * - `endOfFile()`：文件遍历结束时，提交剩余的全部 pending 诊断
 */
class  ReportCommitterDiagnosticComponent(
    session: CfirSession,
    reporter: PendingDiagnosticReporter,
) : AbstractDiagnosticCollectorComponent(session, reporter) {

    override fun visitElement(element: CfirElement, data: CheckerContext) {
        checkAndCommitReportsOn(element, data, commitEverything = false)
    }

    fun endOfFile(file: CfirFile, context: CheckerContext) {
        checkAndCommitReportsOn(file, context, commitEverything = true)

    }
}

