package org.cangnova.cangjie.cfir.resolve.calls.candidate

import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.cfir.types.ConeCangjieType

/**
 * 调用解析诊断基类。
 *
 * 每个诊断携带一个适用性等级，用于更新候选的 lowestApplicability。
 * 具体子类描述各种解析失败原因。
 *
 * 对齐 K2 ResolutionDiagnostic（散落多处），统一收敛到此文件。
 */
abstract class CfirResolutionDiagnostic(
    val applicability: CfirCandidateApplicability,
)

/** 候选被隐藏（不可见的内部 API 等） */
class HiddenCandidate(
    val symbol: CfirSymbol<*>,
) : CfirResolutionDiagnostic(CfirCandidateApplicability.HIDDEN)

/** 实参数量与形参数量不匹配 */
class WrongArgumentCount(
    val expectedCount: Int,
    val actualCount: Int,
) : CfirResolutionDiagnostic(CfirCandidateApplicability.INAPPLICABLE_ARGUMENTS_MAPPING_ERROR)

/** 实参类型与形参类型不兼容 */
class ArgumentTypeMismatch(
    val expectedType: ConeCangjieType,
    val actualType: ConeCangjieType,
    val parameterIndex: Int,
) : CfirResolutionDiagnostic(CfirCandidateApplicability.INAPPLICABLE)

/** 可见性违规（private/protected 等） */
class VisibilityError(
    val symbol: CfirSymbol<*>,
) : CfirResolutionDiagnostic(CfirCandidateApplicability.RESOLVED_WITH_ERROR)
