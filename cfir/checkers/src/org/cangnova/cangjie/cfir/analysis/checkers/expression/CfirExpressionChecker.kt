package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.expressions.CfirStatement

abstract class CfirExpressionChecker<E : CfirStatement> {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    abstract fun check(expression: E)
}


