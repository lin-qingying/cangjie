package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.expressions.CfirErrorExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirNamedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.impl.CfirResolvedAppliedCallableReference
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.source.AbstractCjSourceElement

/**
 * Pattern variable initializer type mismatch checker.
 *
 * Aligned with Kotlin's FirInitializerTypeMismatchChecker behavior for declaration initializers:
 * compare initializer type against declared type and report dedicated diagnostic.
 */
object CfirPatternVariableInitializerTypeMismatchChecker : CfirPatternVariableChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(variable: CfirPatternVariable) {
        val source = variable.source as? AbstractCjSourceElement ?: return

        val expectedType = (variable.returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: return
        val initializer = variable.initializer?.takeIf { it !is CfirErrorExpression } ?: return
        if (initializer.isBareEnumConstructorAccess()) return
        if (initializer is CfirFunctionCall) return
        val actualType = initializer.coneTypeOrNull ?: return

        checkTypeMismatch(
            expectedType = expectedType,
            actualType = actualType,
            source = source,
            preferredSpecializedSource = initializer.source as? AbstractCjSourceElement,
            diagnosticFactory = CfirErrors.PATTERN_INITIALIZER_TYPE_MISMATCH,
        )
    }
}

private fun CfirExpression.isBareEnumConstructorAccess(): Boolean {
    val access = when (this) {
        is CfirNamedAccessExpression -> this
        is CfirQualifiedAccessExpression -> this
        else -> return false
    }

    val symbol = when (val reference = access.calleeReference) {
        is CfirResolvedAppliedCallableReference -> reference.resolvedSymbol
        is CfirResolvedNamedReference -> reference.resolvedSymbol
        else -> return false
    }
    return symbol.takeIf { it.isBound }?.cfir is CfirEnumConstructor
}
