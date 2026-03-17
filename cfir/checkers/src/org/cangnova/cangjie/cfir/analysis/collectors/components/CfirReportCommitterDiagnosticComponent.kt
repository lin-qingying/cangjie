package org.cangnova.cangjie.cfir.analysis.collectors.components

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.diagnostics.PendingDiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.impl.PendingDiagnosticsReporterImpl
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.source.AbstractCjSourceElement

/**
 * 姣忎釜鍏冪礌璁块棶鍚庢彁浜?pending 璇婃柇鍒版渶缁堢殑 DiagnosticCollector銆? *
 * 瀵归綈 K2 `ReportCommitterDiagnosticComponent`銆? *
 * 鎵ц鏃舵満锛? * - `visitElement()` 鈥?鍦ㄥ父瑙勭粍浠舵鏌ュ畬鎴愬悗璋冪敤锛屾彁浜ゅ綋鍓嶅厓绱犵殑 pending 璇婃柇
 * - `endOfFile()` 鈥?鏂囦欢閬嶅巻缁撴潫鏃惰皟鐢紝鎻愪氦鎵€鏈夊墿浣欑殑 pending 璇婃柇
 */
class CfirReportCommitterDiagnosticComponent(
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

