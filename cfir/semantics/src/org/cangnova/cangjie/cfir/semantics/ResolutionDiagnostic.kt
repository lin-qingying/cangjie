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
 * smart cast 不稳定，不能作为候选所需类型使用。
 *
 * @property argument 发生 smart cast 的表达式。
 * @property targetType smart cast 目标类型。
 * @property isCastToNotNull 是否是非空性 smart cast。
 * @property isImplicitInvokeReceiver 是否来自隐式 invoke 接收者。
 */
class UnstableSmartCast(
    val argument: CfirSmartCastExpression,
    val targetType: ConeCangJieType,
    val isCastToNotNull: Boolean,
    val isImplicitInvokeReceiver: Boolean,
) :
    ResolutionDiagnostic(CandidateApplicability.UNSTABLE_SMARTCAST)
