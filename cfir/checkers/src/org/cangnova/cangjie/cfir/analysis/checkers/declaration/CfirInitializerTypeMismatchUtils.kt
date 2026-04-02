package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticFactory3
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeIdealLiteralType
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterLookupTag
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.type.AbstractTypeChecker

context(context: CheckerContext, reporter: DiagnosticReporter)
fun checkTypeMismatch(
    expectedType: ConeCangJieType,
    actualType: ConeCangJieType,
    source: AbstractCjSourceElement,
    diagnosticFactory: CjDiagnosticFactory3<ConeCangJieType, ConeCangJieType, Boolean>,
) {
    if (actualType is ConeErrorType || expectedType is ConeErrorType) return
    val normalizedActualType = actualType.normalizeForSubtypeCheck()
    val normalizedExpectedType = expectedType.normalizeForSubtypeCheck()
    if (AbstractTypeChecker.isSubtypeOf(context.session.typeContext, normalizedActualType, normalizedExpectedType) == true) return
    reporter.reportOn(
        source,
        diagnosticFactory,
        expectedType,
        actualType,
        false,
    )
}

private fun ConeCangJieType.normalizeForSubtypeCheck(): ConeCangJieType {
    return when (this) {
        is ConeTypeVariableType -> {
            val originalTypeParameter = typeConstructor.originalTypeParameter as? ConeTypeParameterLookupTag
            if (originalTypeParameter != null) {
                ConeTypeParameterTypeImpl(originalTypeParameter, attributes)
            } else {
                this
            }
        }

        is ConeIdealLiteralType -> defaultType
        else -> this
    }
}
