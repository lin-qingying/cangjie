package org.cangnova.cangjie.cfir.analysis.collectors.components

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.diagnostics.PendingDiagnosticReporter
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.source.AbstractCjSourceElement

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

    /** 在单个元素检查完成后提交该元素 source 范围内的 pending 诊断。 */
    override fun visitElement(element: CfirElement, data: CheckerContext) {
        checkAndCommitReportsOn(element, data, commitEverything = false)
    }

    /** 在文件遍历结束时使用可定位的文件 source 提交剩余全部 pending 诊断。 */
    fun endOfFile(file: CfirFile, context: CheckerContext) {
        val commitSource = file.commitSourceElement() ?: return
        reporter.checkAndCommitReportsOn(commitSource, context, commitEverything = true)
    }

    /**
     * 文件级 checker 可能挂在 packageDirective 或首个声明上报诊断，
     * 而 LightTree 场景下 [CfirFile.source] 可能为空。
     * 结束文件时必须回退到文件内可定位 source，才能把 pending 诊断真正提交出去。
     */
    private fun CfirFile.commitSourceElement(): AbstractCjSourceElement? {
        return source
            ?: packageDirective.source
            ?: imports.firstOrNull()?.source
            ?: declarations.firstOrNull()?.source
    }
}
