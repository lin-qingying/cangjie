package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.CfirCheckerWithMppKind
import org.cangnova.cangjie.cfir.analysis.checkers.MppCheckerKind
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter

abstract class CfirDeclarationChecker<D : CfirDeclaration>(final override val mppKind: MppCheckerKind) : CfirCheckerWithMppKind {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    abstract fun check(declaration: D)
}

