package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.CheckerDispatchKind
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.expressions.CfirErrorExpression
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.source.AbstractCjSourceElement

/**
 * Field variable initializer type mismatch checker.
 *
 * Validates `let/var/const name: T = expr` for member field variables.
 */
object CfirFieldVariableInitializerTypeMismatchChecker : CfirFieldVariableChecker(CheckerDispatchKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirFieldVariable) {
        val source = declaration.source as? AbstractCjSourceElement ?: return

        val expectedType = (declaration.returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: return
        val initializer = declaration.initializer?.takeIf { it !is CfirErrorExpression } ?: return
        val actualType = initializer.coneTypeOrNull ?: return

        checkTypeMismatch(
            expectedType = expectedType,
            actualType = actualType,
            source = source,
            diagnosticFactory = CfirErrors.TYPE_MISMATCH,
        )
    }
}
