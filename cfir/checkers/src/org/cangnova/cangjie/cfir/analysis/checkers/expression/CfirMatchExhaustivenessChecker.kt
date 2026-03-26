package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.ExhaustivenessAnalyzer
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.ExhaustivenessResult
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.enumMatchTracker
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.reportEnumUsageInMatch
import org.cangnova.cangjie.source.AbstractCjSourceElement

object CfirMatchExhaustivenessChecker : CfirMatchExpressionChecker( ) {

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirMatchExpression) {
        val source = expression.source as? AbstractCjSourceElement ?: return
        val subjectType = expression.subject?.coneTypeOrNull ?: return

        context.session.enumMatchTracker?.reportEnumUsageInMatch(context.containingFilePath, subjectType)

        when (val result = ExhaustivenessAnalyzer.checkMatch(expression, context)) {
            is ExhaustivenessResult.NonExhaustive -> {
                val missingCases = result.getMissingPatternTexts().ifEmpty { listOf("_") }
                reporter.reportOn(source, CfirErrors.NON_EXHAUSTIVE_MATCH, missingCases)
            }
            else -> Unit
        }
    }
}
