package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.CfirTypeCheckUtils
import org.cangnova.cangjie.cfir.analysis.checkers.CheckerDispatchKind
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirIfExpression
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.source.AbstractCjSourceElement

/**
 * `if` 鏉′欢绫诲瀷妫€鏌ワ細鏉′欢琛ㄨ揪寮忓繀椤讳负 Bool銆? */
object CfirIfConditionTypeMismatchChecker : CfirIfExpressionChecker(CheckerDispatchKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirIfExpression) {
        val condition = expression.condition
        val source = condition.source as? AbstractCjSourceElement ?: return
        val actualType = condition.coneTypeOrNull ?: return
        val expectedType = context.session.builtinTypes.boolType

        if (!CfirTypeCheckUtils.isSubtypeOf(actualType, expectedType)) {
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

