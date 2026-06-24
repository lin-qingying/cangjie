package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter

/**
 * 对齐官方 CFFI：`type` 展开为 `CFunc<...>` 时，非法参数/返回类型仍应落在别名内部的具体类型节点上，
 * 而不是等到 `foreign func` 使用该别名时退化为外层 use-site 诊断。
 */
object CfirTypeAliasCFuncLegalityChecker : CfirTypeAliasChecker() {
    /**
     * 检查 typealias 展开类型是否为 CFunc，并报告嵌套类型上的 C 互操作非法诊断。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirTypeAlias) {
        with(context) {
            with(reporter) {
                CfirCFuncTypeLegalityReporter.reportNestedDiagnosticsIfCFunc(declaration.expandedTypeRef)
            }
        }
    }
}
