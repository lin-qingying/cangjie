package org.cangnova.cangjie.cfir.semantics

import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.resolve.calls.tower.ApplicabilityDetail
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability
import org.cangnova.cangjie.resolve.calls.tower.isSuccess

abstract class ResolutionDiagnostic(
    val applicability: CandidateApplicability,
)

@OptIn(ApplicabilityDetail::class)
val  ResolutionDiagnostic.isSuccess: Boolean get() = applicability.isSuccess
