package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.MppCheckerKind
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirErrorExpression
import org.cangnova.cangjie.cfir.source.AbstractCjSourceElement

object CfirErrorExpressionDiagnosticChecker : CfirErrorExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirErrorExpression) {
        val source = expression.source as? AbstractCjSourceElement ?: return
        reporter.reportOn(source, CfirErrors.INVALID_DECLARATION, expression.reason)
    }
}

object CfirBasicExpressionCheckers : CfirExpressionCheckers() {
    override val errorExpressionCheckers: Set<CfirErrorExpressionChecker>
        get() = setOf(CfirErrorExpressionDiagnosticChecker)

    override val matchExpressionCheckers: Set<CfirMatchExpressionChecker>
        get() = setOf(CfirMatchExhaustivenessChecker)
}

