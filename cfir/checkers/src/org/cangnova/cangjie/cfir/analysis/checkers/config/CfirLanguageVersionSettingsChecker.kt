package org.cangnova.cangjie.cfir.analysis.checkers.config

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter

/** 不依赖具体语法节点、只检查语言版本设置和 session 配置的 CFIR checker 基类。 */
abstract class CfirLanguageVersionSettingsChecker {
    /** 在当前 checker 上下文中执行全局语言版本设置检查，并通过 reporter 上报诊断。 */
    context(context: CheckerContext)
    abstract fun check(reporter: DiagnosticReporter)
}
