package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.expressions.CfirTryExpression

/**
 * 对齐官方仓颉 `ChkTryExpr/ChkTryExprCatchesAndHandles + ChkBlock`：
 * target-typed `try` 会把外层目标类型分别下推到 try/catch block 的尾表达式，
 * 类型不匹配时主诊断落在具体尾表达式上，而不是落回整个 `return try`。
 */
object CfirTryTargetTypeMismatchChecker : CfirTryExpressionChecker() {
    /** 检查 target-typed try/catch block 尾表达式是否符合外层期望类型。 */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirTryExpression) {
        val expectedType = expression.expectedTypeFromTargetContext(context) ?: return
        checkTargetTypedExpression(expression, expectedType)
    }
}
