package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirIfExpression
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * `if` 条件类型检查：条件表达式必须为 `Bool`。
 */
object CfirIfConditionTypeMismatchChecker : CfirIfExpressionChecker( ) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirIfExpression) {
        val condition = expression.condition
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
