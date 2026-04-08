package org.cangnova.cangjie.cfir.diagnostic

import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.semantics.ResolutionDiagnostic
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability

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

/**
 * 对齐 Kotlin FIR 的参数映射错误分层：
 * 参数绑定阶段先产出结构化 ResolutionDiagnostic，
 * 后续统一通过 coneDiagnosticToCfirDiagnostic 做前端诊断映射。
 */
class NoValueForParameter(
    val valueParameter: CfirValueParameter,
) : ResolutionDiagnostic(CandidateApplicability.INAPPLICABLE_ARGUMENTS_MAPPING_ERROR)

class TooManyArguments(
    val argument: CfirExpression,
    val targetName: Name,
) : ResolutionDiagnostic(CandidateApplicability.INAPPLICABLE_ARGUMENTS_MAPPING_ERROR)

class NamedParameterNotFound(
    val argument: CfirExpression,
    val name: Name,
) : ResolutionDiagnostic(CandidateApplicability.INAPPLICABLE_ARGUMENTS_MAPPING_ERROR)

class ArgumentPassedTwice(
    val argument: CfirExpression,
    val parameter: CfirValueParameter,
) : ResolutionDiagnostic(CandidateApplicability.INAPPLICABLE_ARGUMENTS_MAPPING_ERROR)

class NamedArgumentsNotAllowed(
    val argument: CfirExpression,
    val targetDescription: String,
) : ResolutionDiagnostic(CandidateApplicability.INAPPLICABLE_ARGUMENTS_MAPPING_ERROR)

class MixingNamedAndPositionalArguments(
    val argument: CfirExpression,
) : ResolutionDiagnostic(CandidateApplicability.INAPPLICABLE_ARGUMENTS_MAPPING_ERROR)

class NeedNamedArgument(
    val argument: CfirExpression,
    val parameter: CfirValueParameter,
) : ResolutionDiagnostic(CandidateApplicability.INAPPLICABLE_ARGUMENTS_MAPPING_ERROR)

class VisibilityError(
    val symbol: CfirSymbol<*>,
) : ResolutionDiagnostic(CandidateApplicability.RESOLVED_WITH_ERROR)

class InferenceConstraintError(
    val message: String,
) : ResolutionDiagnostic(CandidateApplicability.INAPPLICABLE)

object ResolutionResultOverridesOtherToPreserveCompatibility :
    ResolutionDiagnostic(CandidateApplicability.RESOLVED_NEED_PRESERVE_COMPATIBILITY)
