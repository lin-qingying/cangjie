package org.cangnova.cangjie.cfir.diagnostic

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.semantics.ResolutionDiagnostic
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability

/**
 * 调用实参类型与形参期望类型不匹配。
 *
 * @property expectedType 形参或上下文期望类型。
 * @property actualType 实参实际类型。
 * @property argument 触发错误的实参表达式。
 * @property isMismatchDueToNullability 不匹配是否仅由可空性差异导致。
 * @property anonymousFunctionIfReturnExpression 当实参检查复用于 lambda 返回检查时，保存所属匿名函数。
 * @property systemHadContradiction 类型约束系统是否已经存在矛盾。
 */
class ArgumentTypeMismatch(
    /**
     * 形参或上下文期望类型。
     */
    val expectedType: ConeCangJieType,
    /**
     * 实参实际类型。
     */
    val actualType: ConeCangJieType,
    /**
     * 触发错误的实参表达式。
     */
    val argument: CfirExpression,
    /**
     * 不匹配是否仅由可空性差异导致。
     */
    val isMismatchDueToNullability: Boolean,
    // 用于 lambda 返回语句的类型不匹配报告（对齐 K2 anonymousFunctionIfReturnExpression）
    /**
     * 当实参检查复用于 lambda 返回检查时，保存所属匿名函数。
     */
    val anonymousFunctionIfReturnExpression: CfirFunction? = null,
    /**
     * 类型约束系统是否已经存在矛盾。
     */
    val systemHadContradiction: Boolean = false,
) : ResolutionDiagnostic(CandidateApplicability.INAPPLICABLE)

/**
 * 官方 Cangjie `TypeCheckCall::GetArgTyPossibilities` 在调用实参是重载函数引用、
 * 且外层调用只能从实参类型反推泛型时，把错误归属到外层调用本身。
 *
 * @property callSite 需要承载错误的外层调用节点。
 * @property argument 触发歧义的实参表达式。
 */
class AmbiguousArgumentType(
    /**
     * 需要承载错误的外层调用节点。
     */
    val callSite: CfirElement,
    /**
     * 触发歧义的实参表达式。
     */
    val argument: CfirExpression,
) : ResolutionDiagnostic(CandidateApplicability.INAPPLICABLE)

/**
 * 候选被隐藏，不能参与最终解析。
 */
class HiddenCandidate : ResolutionDiagnostic(CandidateApplicability.HIDDEN)

/**
 * 调用实参数量与候选形参数量不匹配。
 *
 * @property expectedCount 候选期望的参数数量。
 * @property actualCount 调用实际提供的参数数量。
 */
class WrongArgumentCount(
    /**
     * 候选期望的参数数量。
     */
    val expectedCount: Int,
    /**
     * 调用实际提供的参数数量。
     */
    val actualCount: Int,
) : ResolutionDiagnostic(CandidateApplicability.INAPPLICABLE_ARGUMENTS_MAPPING_ERROR)

/**
 * 对齐 Kotlin FIR 的参数映射错误分层：
 * 参数绑定阶段先产出结构化 ResolutionDiagnostic，
 * 后续统一通过 coneDiagnosticToCfirDiagnostic 做前端诊断映射。
 *
 * @property valueParameter 未获得实参的形参声明。
 */
class NoValueForParameter(
    /**
     * 未获得实参的形参声明。
     */
    val valueParameter: CfirValueParameter,
) : ResolutionDiagnostic(CandidateApplicability.INAPPLICABLE_ARGUMENTS_MAPPING_ERROR)

/**
 * 调用提供了多余实参。
 *
 * @property argument 多余的实参表达式。
 * @property targetName 被调用目标名称。
 */
class TooManyArguments(
    /**
     * 多余的实参表达式。
     */
    val argument: CfirExpression,
    /**
     * 被调用目标名称。
     */
    val targetName: Name,
) : ResolutionDiagnostic(CandidateApplicability.INAPPLICABLE_ARGUMENTS_MAPPING_ERROR)

/**
 * 命名实参找不到对应形参。
 *
 * @property argument 触发错误的命名实参表达式。
 * @property name 不存在的形参名。
 */
class NamedParameterNotFound(
    /**
     * 触发错误的命名实参表达式。
     */
    val argument: CfirExpression,
    /**
     * 不存在的形参名。
     */
    val name: Name,
) : ResolutionDiagnostic(CandidateApplicability.INAPPLICABLE_ARGUMENTS_MAPPING_ERROR)

/**
 * 对齐 Kotlin FIR：当候选自身没有更细粒度的结构化错误类型时，
 * 仍然需要一个统一的“不可适用候选”诊断挂到 Candidate 上，
 * 以便 overload 收集、IDE all-candidates 查询、错误引用回写使用同一语义锚点。
 */
object InapplicableCandidate : ResolutionDiagnostic(CandidateApplicability.INAPPLICABLE)

/**
 * 函数引用实参在当前期望函数类型下没有可适用声明。
 *
 * 该诊断对齐 Kotlin FIR `UnsuccessfulCallableReferenceArgument`：resolve 阶段记录
 * callable reference 实参的失败根因和源码锚点，用户可见的 no-match-ref 诊断由 checker 映射层统一生成。
 *
 * @property argument 作为函数引用使用、但没有匹配声明的实参表达式。
 */
class UnsuccessfulCallableReferenceArgument(
    /**
     * 作为函数引用使用、但没有匹配声明的实参表达式。
     */
    val argument: CfirExpression,
) : ResolutionDiagnostic(CandidateApplicability.INAPPLICABLE)

/**
 * 接收者类型不适用。
 *
 * 对齐 Kotlin FIR `InapplicableWrongReceiver`，用于把 receiver 约束失败与普通参数
 * 类型失败分层，保证 tower resolve 的候选排序使用 `INAPPLICABLE_WRONG_RECEIVER`。
 *
 * @property expectedType 候选期望的接收者类型。
 * @property actualType 调用侧实际接收者类型。
 */
class InapplicableWrongReceiver(
    /**
     * 候选期望的接收者类型。
     */
    val expectedType: ConeCangJieType? = null,
    /**
     * 调用侧实际接收者类型。
     */
    val actualType: ConeCangJieType? = null,
) : ResolutionDiagnostic(CandidateApplicability.INAPPLICABLE_WRONG_RECEIVER)

/**
 * 同一个形参被重复传参。
 *
 * @property argument 重复传入的实参表达式。
 * @property parameter 被重复绑定的形参。
 */
class ArgumentPassedTwice(
    /**
     * 重复传入的实参表达式。
     */
    val argument: CfirExpression,
    /**
     * 被重复绑定的形参。
     */
    val parameter: CfirValueParameter,
) : ResolutionDiagnostic(CandidateApplicability.INAPPLICABLE_ARGUMENTS_MAPPING_ERROR)

/**
 * 目标调用不允许命名实参。
 *
 * @property argument 使用命名形式的实参表达式。
 * @property targetDescription 目标调用的可读描述。
 */
class NamedArgumentsNotAllowed(
    /**
     * 使用命名形式的实参表达式。
     */
    val argument: CfirExpression,
    /**
     * 目标调用的可读描述。
     */
    val targetDescription: String,
) : ResolutionDiagnostic(CandidateApplicability.INAPPLICABLE_ARGUMENTS_MAPPING_ERROR)

/**
 * 调用混用了命名实参和位置实参。
 *
 * @property argument 触发混用错误的实参表达式。
 */
class MixingNamedAndPositionalArguments(
    /**
     * 触发混用错误的实参表达式。
     */
    val argument: CfirExpression,
) : ResolutionDiagnostic(CandidateApplicability.INAPPLICABLE_ARGUMENTS_MAPPING_ERROR)

/**
 * 某个实参必须以命名形式传入。
 *
 * @property argument 当前未命名的实参表达式。
 * @property parameter 要求命名传入的形参。
 */
class NeedNamedArgument(
    /**
     * 当前未命名的实参表达式。
     */
    val argument: CfirExpression,
    /**
     * 要求命名传入的形参。
     */
    val parameter: CfirValueParameter,
) : ResolutionDiagnostic(CandidateApplicability.INAPPLICABLE_ARGUMENTS_MAPPING_ERROR)

/**
 * 候选符号可见性失败。
 *
 * @property symbol 不可访问的符号。
 */
class VisibilityError(
    /**
     * 不可访问的符号。
     */
    val symbol: CfirBasedSymbol<*>,
) : ResolutionDiagnostic(CandidateApplicability.RESOLVED_WITH_ERROR)

/**
 * 类型推断约束错误。
 *
 * @property message 约束系统给出的错误消息。
 */
class InferenceConstraintError(
    /**
     * 约束系统给出的错误消息。
     */
    val message: String,
) : ResolutionDiagnostic(CandidateApplicability.INAPPLICABLE)

/**
 * 为兼容性保留的解析结果覆盖了其他结果。
 */
object ResolutionResultOverridesOtherToPreserveCompatibility :
    ResolutionDiagnostic(CandidateApplicability.RESOLVED_NEED_PRESERVE_COMPATIBILITY)
