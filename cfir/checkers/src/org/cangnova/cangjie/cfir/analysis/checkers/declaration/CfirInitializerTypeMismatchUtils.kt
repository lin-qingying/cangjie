package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.CfirTypeCheckUtils
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticFactory3
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.source.AbstractCjSourceElement

context(context: CheckerContext, reporter: DiagnosticReporter)
fun checkTypeMismatch(
    expectedType: ConeCangjieType,
    actualType: ConeCangjieType,
    source: AbstractCjSourceElement,
    diagnosticFactory: CjDiagnosticFactory3<ConeCangjieType, ConeCangjieType, Boolean>,
) {
    if (CfirTypeCheckUtils.isSubtypeOf(actualType, expectedType)) return
    reporter.reportOn(
        source,
        diagnosticFactory,
        expectedType,
        actualType,
        false,
    )
}

