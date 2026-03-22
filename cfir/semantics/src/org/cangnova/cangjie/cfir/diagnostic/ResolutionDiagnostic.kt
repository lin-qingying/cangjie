package org.cangnova.cangjie.cfir.diagnostic

import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.semantics.CandidateApplicability
import org.cangnova.cangjie.cfir.semantics.ResolutionDiagnostic
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType

class  ArgumentTypeMismatch(
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

class HiddenCandidate : ResolutionDiagnostic(CandidateApplicability.HIDDEN)

class WrongArgumentCount(
    val expectedCount: Int,
    val actualCount: Int,
) : ResolutionDiagnostic(CandidateApplicability.INAPPLICABLE_ARGUMENTS_MAPPING_ERROR)

class VisibilityError(
    val symbol: CfirSymbol<*>,
) : ResolutionDiagnostic(CandidateApplicability.RESOLVED_WITH_ERROR)

class InferenceConstraintError(
    val message: String,
) : ResolutionDiagnostic(CandidateApplicability.INAPPLICABLE)
