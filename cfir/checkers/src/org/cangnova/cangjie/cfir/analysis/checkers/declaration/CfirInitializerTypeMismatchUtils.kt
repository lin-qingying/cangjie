package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticFactory3
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.typeContext
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
    if (AbstractTypeChecker.isSubtypeOf(context.session.typeContext, actualType, expectedType) == true) return
    reporter.reportOn(
        source,
        diagnosticFactory,
        expectedType,
        actualType,
        false,
    )
}
