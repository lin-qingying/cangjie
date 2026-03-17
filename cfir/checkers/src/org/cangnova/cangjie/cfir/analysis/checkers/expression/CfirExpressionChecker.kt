package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.CfirCheckerWithDispatchKind
import org.cangnova.cangjie.cfir.analysis.checkers.CheckerDispatchKind
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.expressions.CfirStatement

abstract class CfirExpressionChecker<E : CfirStatement>(final override val dispatchKind: CheckerDispatchKind) : CfirCheckerWithDispatchKind {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    abstract fun check(expression: E)
}


