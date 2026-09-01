package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.expression.checkTargetTypedExpression
import org.cangnova.cangjie.cfir.analysis.checkers.expression.explicitDeclaredTypeOrNull
import org.cangnova.cangjie.cfir.analysis.checkers.expression.isHandled
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.expressions.CfirErrorExpression
import org.cangnova.cangjie.source.AbstractCjSourceElement

/**
 * 字段变量初始化器类型检查器。
 *
 * 检查成员字段 `let/var/const name: T = expr` 中初始化表达式是否可以赋给声明类型。
 */
object CfirFieldVariableInitializerTypeMismatchChecker : CfirFieldVariableChecker() {
    /**
     * 检查字段声明的初始化器类型。
     *
     * 错误表达式和未解析类型不重复报告；实际诊断优先落在初始化器表达式源码上。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirFieldVariable) {
        val source = declaration.source as? AbstractCjSourceElement ?: return

        val expectedType = declaration.explicitDeclaredTypeOrNull() ?: return
        val initializer = declaration.initializer?.takeIf { it !is CfirErrorExpression } ?: return
        if (checkTargetTypedExpression(initializer, expectedType).isHandled) return
        val actualType = initializer.coneTypeOrNull ?: return

        checkTypeMismatch(
            expectedType = expectedType,
            actualType = actualType,
            expression = initializer,
            source = source,
            preferredSpecializedSource = initializer.source as? AbstractCjSourceElement,
            diagnosticFactory = CfirErrors.TYPE_MISMATCH,
        )
    }
}
