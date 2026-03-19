package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.CheckerDispatchKind
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.expressions.CfirErrorExpression
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.source.AbstractCjSourceElement

/**
 * Pattern variable initializer type mismatch checker.
 *
 * Aligned with Kotlin's FirInitializerTypeMismatchChecker behavior for declaration initializers:
 * compare initializer type against declared type and report dedicated diagnostic.
 */
object CfirPatternVariableInitializerTypeMismatchChecker : CfirPatternVariableChecker(CheckerDispatchKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(variable: CfirPatternVariable) {
        val source = variable.source as? AbstractCjSourceElement ?: return

        val expectedType = (variable.returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: return
        val initializer = variable.initializer?.takeIf { it !is CfirErrorExpression } ?: return
        val actualType = initializer.coneTypeOrNull ?: return

        checkTypeMismatch(
            expectedType = expectedType,
            actualType = actualType,
            source = source,
            diagnosticFactory = CfirErrors.PATTERN_INITIALIZER_TYPE_MISMATCH,
        )
    }
}
