package org.cangjie.cfir.diagnostics

import org.cangjie.cfir.session.CfirSessionComponent

/**
 * 诊断报告接口。
 */
interface CfirDiagnosticReporter : CfirSessionComponent {
    fun report(diagnostic: CfirDiagnostic)
    val hasErrors: Boolean
}
