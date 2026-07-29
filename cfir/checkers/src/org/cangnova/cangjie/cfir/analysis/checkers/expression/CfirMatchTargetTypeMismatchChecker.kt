package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression

/**
 * target-typed `match` 分支体类型检查。
 *
 * 官方 `ChkMatchExpr` 在带目标类型时会对每个 case action 执行 `Check(ctx, target, action)`；
 * CFIR resolve 已经把 expected type 下传给分支体，这里负责在真实尾表达式上补齐诊断落点。
 */
object CfirMatchTargetTypeMismatchChecker : CfirMatchExpressionChecker() {
    /** 检查每个 `match` 分支体尾表达式是否满足外层目标类型。 */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirMatchExpression) {
        val expectedType = expression.expectedTypeFromTargetContext(context) ?: return
        checkTargetTypedExpression(expression, expectedType)
    }
}
