package org.cangnova.cangjie.cfir.semantics

import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.types.ConeCangJieType

abstract class ResolutionDiagnostic(
    val applicability: CandidateApplicability,
)

val  ResolutionDiagnostic.isSuccess: Boolean get() = applicability.isSuccess
