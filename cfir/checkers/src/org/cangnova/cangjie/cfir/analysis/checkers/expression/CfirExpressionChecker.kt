package org.cangjie.cfir.analysis.checkers.expression

import org.cangjie.cfir.analysis.checkers.CfirCheckerWithMppKind
import org.cangjie.cfir.analysis.checkers.MppCheckerKind
import org.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangjie.cfir.expressions.CfirStatement

abstract class CfirExpressionChecker<E : CfirStatement>(final override val mppKind: MppCheckerKind) : CfirCheckerWithMppKind {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    abstract fun check(expression: E)
}

