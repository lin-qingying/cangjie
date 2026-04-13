package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction

/**
 * Inout 语义检查器
 *
 * 对齐 C++ FFI/CFFICheck.cpp:
 * - inout 参数只能在 CFunc 调用上下文中使用
 *
 * inout 表达式在 CFIR 中体现为函数调用的参数标记。
 * 当前实现框架就绪，具体 inout 参数标识逻辑待 CFIR 模型中
 * inout 参数标记确立后启用。
 */
object CfirInoutSemanticsChecker : CfirFunctionCallChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirFunctionCall) {
        // inout 参数检查框架
        // 当前 CFIR 模型中 inout 参数尚未有显式标记（如 isInout），
        // 待 CFIR 树模型补充 inout 参数信息后启用检查逻辑。
        //
        // 未来实现：
        // 1. 遍历 expression.argumentList 找 inout 标记的参数
        // 2. 验证调用目标是 foreign/CFunc 函数
        // 3. 验证 inout 参数是 var 变量引用
    }
}
