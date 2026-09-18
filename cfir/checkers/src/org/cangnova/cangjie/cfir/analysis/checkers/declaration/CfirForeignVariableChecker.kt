package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn

/**
 * 对齐官方 CFFICheck::PreCheckAnnoForCFFI 的顶层变量规则。
 *
 * `foreign` 对变量的语法是合法的，但不能把变量自动当成 C ABI 声明；
 * 只有显式 `@C` 才能继续进入 C 互操作语义。修饰符目标 checker 只负责
 * 保留这个合法语法入口，本 checker 负责发布唯一的结构化诊断。
 */
object CfirForeignVariableChecker : CfirBasicDeclarationChecker() {
    context(context: org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: org.cangnova.cangjie.cfir.declarations.CfirDeclaration) {
        val variable = declaration as? CfirVariable ?: return
        if (!variable.status.isForeign || variable.status.isC) return

        reporter.reportOn(
            source = variable.source,
            factory = CfirErrors.NATIVE_VAR_ERROR,
        )
    }
}
