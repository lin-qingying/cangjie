package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.MppCheckerKind
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirInvalidDeclaration
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.source.AbstractCjSourceElement

object CfirInvalidDeclarationReasonChecker : CfirInvalidDeclarationChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirInvalidDeclaration) {
        val source = declaration.source as? AbstractCjSourceElement ?: return
        reporter.reportOn(source, CfirErrors.INVALID_DECLARATION, declaration.reason)
    }
}

object CfirBasicDeclarationCheckers : CfirDeclarationCheckers() {
    override val invalidDeclarationCheckers: Set<CfirInvalidDeclarationChecker>
        get() = setOf(CfirInvalidDeclarationReasonChecker)
}
