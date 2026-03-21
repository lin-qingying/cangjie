package org.cangnova.cangjie.cfir.semantics

import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.types.ConeCangJieType

abstract class ResolutionDiagnostic(
    val applicability: CandidateApplicability,
)

val  ResolutionDiagnostic.isSuccess: Boolean get() = applicability.isSuccess
class ArgumentTypeMismatch(
    val expectedType: ConeCangJieType,
    val actualType: ConeCangJieType,
    val argument: CfirExpression,
    val isMismatchDueToNullability: Boolean,
    // We use argument checking mechanism for return statements of lambdas, too.
    // Thus, to report proper RETURN_TYPE_MISMATCH we preserve a reference to the lambda
    // 用于 lambda 返回语句的类型不匹配报告（对齐 K2 anonymousFunctionIfReturnExpression）
    val anonymousFunctionIfReturnExpression: CfirFunction? = null,
    val systemHadContradiction: Boolean = false,
) : ResolutionDiagnostic( CandidateApplicability.INAPPLICABLE)
