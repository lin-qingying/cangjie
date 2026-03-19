package org.cangnova.cangjie.cfir.resolve.calls.candidate

import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.cfir.types.ConeCangjieType

/**
 * 调用解析诊断基类。
 * 每个诊断都携带一个适用性等级，用于更新候选的 `lowestApplicability`。
 * 具体子类分别描述不同的解析失败原因。
 * 对齐 K2 `ResolutionDiagnostic`，这里把相关诊断集中放在一个文件里。
 */
abstract class CfirResolutionDiagnostic(
    val applicability: CfirCandidateApplicability,
)

/** 候选被隐藏，如访问了不可见的内部 API。 */
class HiddenCandidate(
    val symbol: CfirSymbol<*>,
) : CfirResolutionDiagnostic(CfirCandidateApplicability.HIDDEN)

/** 实参数量与形参数量不匹配。 */
class WrongArgumentCount(
    val expectedCount: Int,
    val actualCount: Int,
) : CfirResolutionDiagnostic(CfirCandidateApplicability.INAPPLICABLE_ARGUMENTS_MAPPING_ERROR)

/** 实参类型与形参类型不兼容。 */
class ArgumentTypeMismatch(
    val expectedType: ConeCangjieType,
    val actualType: ConeCangjieType,
    val parameterIndex: Int,
) : CfirResolutionDiagnostic(CfirCandidateApplicability.INAPPLICABLE)

/** 可见性违规，例如访问到了 `private` / `protected` 成员。 */
class VisibilityError(
    val symbol: CfirSymbol<*>,
) : CfirResolutionDiagnostic(CfirCandidateApplicability.RESOLVED_WITH_ERROR)

/** 泛型推断中的约束系统错误。 */
class InferenceConstraintError(
    val message: String,
) : CfirResolutionDiagnostic(CfirCandidateApplicability.INAPPLICABLE)

