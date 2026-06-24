package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors

import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirForInExpression
import org.cangnova.cangjie.cfir.expressions.CfirLoopExpression
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * `while` / `do-while` 条件类型检查：条件表达式必须为 `Bool`。
 * `for-in` 没有用户可写的布尔条件，因此直接跳过。
 */
object CfirLoopConditionTypeMismatchChecker : CfirBasicExpressionChecker() {
    /** 检查循环条件表达式类型是否可作为 `Bool` 使用。 */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirStatement) {
        val loopExpression = expression as? CfirLoopExpression ?: return
        if (loopExpression is CfirForInExpression) return

        val condition = loopExpression.condition
        val source = condition.source as? AbstractCjSourceElement ?: return
        val actualType = condition.coneTypeOrNull ?: return
        val expectedType = context.session.builtinTypes.boolType

        if (AbstractTypeChecker.isSubtypeOf(context.session.typeContext, actualType, expectedType) != true) {
            reporter.reportOn(
                source,
                CfirErrors.TYPE_MISMATCH,
                expectedType,
                actualType,
                false,
            )
        }
    }
}
