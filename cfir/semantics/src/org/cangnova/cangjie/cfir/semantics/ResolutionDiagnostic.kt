package org.cangnova.cangjie.cfir.semantics

import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirSmartCastExpression
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.resolve.calls.tower.ApplicabilityDetail
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability
import org.cangnova.cangjie.resolve.calls.tower.isSuccess

/**
 * 候选解析阶段产生的结构化诊断基类。
 *
 * @property applicability 该诊断对候选适用性造成的影响。
 */
abstract class ResolutionDiagnostic(
    /**
     * 该诊断对候选适用性造成的影响。
     */
    val applicability: CandidateApplicability,
)

@OptIn(ApplicabilityDetail::class)
/** 当前 resolution diagnostic 是否仍属于成功适用性层级。 */
val ResolutionDiagnostic.isSuccess: Boolean get() = applicability.isSuccess

/**
 * 实参类型中出现错误类型，导致候选不可适用。
 */
object ErrorTypeInArguments : ResolutionDiagnostic(CandidateApplicability.INAPPLICABLE)

/**
 * 候选 callable 的声明签名中出现普通错误类型，调用诊断按声明错误恢复。
 *
 * 该诊断只阻止错误类型进入普通 subtype constraint，不在调用位置产生派生 no-match；
 * 声明处的根诊断已经完整描述错误。
 */
object ErrorTypeInCandidateSignature : ResolutionDiagnostic(CandidateApplicability.INAPPLICABLE)

/**
 * 候选 callable 的参数签名来自重声明 classifier 的类型使用歧义。
 *
 * 官方语义会在这种声明签名无法确定唯一类型时保留调用 no-match，必须与普通未声明类型等
 * 可恢复的签名错误分离。
 */
object AmbiguousClassifierTypeInCandidateSignature : ResolutionDiagnostic(CandidateApplicability.INAPPLICABLE)

/**
 * smart cast 不稳定，不能作为候选所需类型使用。
 *
 * @property argument 发生 smart cast 的表达式。
 * @property targetType smart cast 目标类型。
 * @property isCastToNotNull 是否是非空性 smart cast。
 * @property isImplicitInvokeReceiver 是否来自隐式 invoke 接收者。
 */
class UnstableSmartCast(
    /**
     * 发生 smart cast 的表达式。
     */
    val argument: CfirSmartCastExpression,
    /**
     * smart cast 目标类型。
     */
    val targetType: ConeCangJieType,
    /**
     * 是否是非空性 smart cast。
     */
    val isCastToNotNull: Boolean,
    /**
     * 是否来自隐式 invoke 接收者。
     */
    val isImplicitInvokeReceiver: Boolean,
) :
    ResolutionDiagnostic(CandidateApplicability.UNSTABLE_SMARTCAST)
