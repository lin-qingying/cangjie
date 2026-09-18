package org.cangnova.cangjie.cfir.analysis.collectors.components

import org.cangnova.cangjie.cfir.analysis.checkersComponent
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.diagnostics.PendingDiagnosticReporter
import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * 对齐 Kotlin FIR 的 settings-level checker 入口。
 *
 * 该组件只消费当前 session 注册的 settings checker，不在 parser 或声明 checker
 * 中重复读取版本配置；没有 checker 时也必须保留这条稳定的 phase seam。
 */
class LanguageVersionSettingsDiagnosticComponent(
    session: CfirSession,
    reporter: PendingDiagnosticReporter,
) : AbstractDiagnosticCollectorComponent(session, reporter) {
    override fun checkSettings(data: CheckerContext) {
        with(data) {
            for (checker in session.checkersComponent.languageVersionSettingsCheckers.languageVersionSettingsCheckers) {
                checker.check(reporter)
            }
        }
    }
}
