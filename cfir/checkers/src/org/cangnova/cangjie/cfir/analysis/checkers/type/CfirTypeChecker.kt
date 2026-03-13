package org.cangjie.cfir.analysis.checkers.type

import org.cangjie.cfir.analysis.checkers.CfirCheckerWithMppKind
import org.cangjie.cfir.analysis.checkers.MppCheckerKind
import org.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangjie.cfir.types.CfirTypeRef

abstract class CfirTypeChecker<T : CfirTypeRef>(final override val mppKind: MppCheckerKind) : CfirCheckerWithMppKind {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    abstract fun check(typeRef: T)
}

