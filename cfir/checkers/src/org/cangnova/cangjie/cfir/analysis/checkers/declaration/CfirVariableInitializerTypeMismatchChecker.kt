package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.MppCheckerKind
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.expression.checkTargetTypedExpression
import org.cangnova.cangjie.cfir.analysis.checkers.expression.explicitDeclaredTypeOrNull
import org.cangnova.cangjie.cfir.analysis.checkers.expression.isHandled
import org.cangnova.cangjie.cfir.analysis.checkers.expression.TargetTypedCheckOutcome
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirResourceVariable
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.expressions.CfirErrorExpression
import org.cangnova.cangjie.source.AbstractCjSourceElement

/**
 * 拥有普通初始化器的变量类型检查器。
 *
 * 字段和资源共用显式声明类型与 initializer 的兼容性协议；模式声明保留其 enum/投影专用检查。
 */
object CfirVariableInitializerTypeMismatchChecker : CfirCallableDeclarationChecker(MppCheckerKind.Common) {
    /**
     * 检查字段和资源声明的初始化器类型。
     *
     * 错误表达式和未解析类型不重复报告；实际诊断优先落在初始化器表达式源码上。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirCallableDeclaration) {
        val variable = when (declaration) {
            is CfirFieldVariable -> declaration
            is CfirResourceVariable -> declaration
            else -> return
        }
        checkVariableInitializerTypeMismatch(variable)
    }
}

/** 同一判定同时服务于诊断报告与初始化流；类型检查失败的值不再派生初始化前使用错误。 */
context(context: CheckerContext, reporter: DiagnosticReporter)
internal fun checkVariableInitializerTypeMismatch(variable: CfirVariable): Boolean {
    val source = variable.source as? AbstractCjSourceElement ?: return false
    val expectedType = variable.explicitDeclaredTypeOrNull() ?: return false
    val initializer = variable.initializer?.takeIf { it !is CfirErrorExpression } ?: return false
    val targetOutcome = checkTargetTypedExpression(initializer, expectedType)
    if (targetOutcome.isHandled) return (targetOutcome as TargetTypedCheckOutcome.Handled).reported
    val actualType = initializer.coneTypeOrNull ?: return false

    return checkTypeMismatch(
        expectedType = expectedType,
        actualType = actualType,
        expression = initializer,
        source = source,
        preferredSpecializedSource = initializer.source as? AbstractCjSourceElement,
        diagnosticFactory = CfirErrors.TYPE_MISMATCH,
    )
}
