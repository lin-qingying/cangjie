package org.cangnova.cangjie.cfir.analysis.checkers.type

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.types.CfirTypeRef

abstract class CfirTypeChecker<T : CfirTypeRef> {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    abstract fun check(typeRef: T)
}


