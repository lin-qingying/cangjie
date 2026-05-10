package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.enumMatchTracker
import org.cangnova.cangjie.cfir.expressions.CfirMatchExhaustivenessStatus
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.ExhaustivenessAnalyzer
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.ExhaustivenessResult
import org.cangnova.cangjie.cfir.reportEnumUsageInMatch
import org.cangnova.cangjie.source.AbstractCjSourceElement

object CfirMatchExhaustivenessChecker : CfirMatchExpressionChecker( ) {

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirMatchExpression) {
        val source = expression.source as? AbstractCjSourceElement ?: return
        val subjectType = expression.subject?.coneTypeOrNull ?: return
        if (expression.hasPatternLegalityProblem(context)) return

        context.session.enumMatchTracker?.reportEnumUsageInMatch(context.containingFilePath, subjectType)

        val exhaustiveness = checkerExhaustivenessStatus(expression, context)
        if (exhaustiveness !is CfirMatchExhaustivenessStatus.NonExhaustive) return
        reporter.reportOn(source, CfirErrors.NON_EXHAUSTIVE_MATCH, exhaustiveness.missingCaseTexts)
    }

    private fun checkerExhaustivenessStatus(
        expression: CfirMatchExpression,
        context: CheckerContext,
    ): CfirMatchExhaustivenessStatus {
        val cached = expression.exhaustiveness
        if (cached !is CfirMatchExhaustivenessStatus.Unknown) {
            return cached
        }

        val resolved = when (val result = ExhaustivenessAnalyzer.checkMatch(expression, context.session)) {
            ExhaustivenessResult.Exhaustive -> CfirMatchExhaustivenessStatus.Exhaustive(
                source = CfirMatchExhaustivenessStatus.Source.Checker,
            )

            is ExhaustivenessResult.NonExhaustive -> CfirMatchExhaustivenessStatus.NonExhaustive(
                missingCaseTexts = result.getMissingPatternTexts(),
                source = CfirMatchExhaustivenessStatus.Source.Checker,
            )

            is ExhaustivenessResult.Error,
            ExhaustivenessResult.Skipped,
            -> CfirMatchExhaustivenessStatus.Unknown
        }
        expression.replaceExhaustiveness(resolved)
        return resolved
    }
}
