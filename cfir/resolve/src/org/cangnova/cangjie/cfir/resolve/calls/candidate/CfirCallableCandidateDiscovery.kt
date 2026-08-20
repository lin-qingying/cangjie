package org.cangnova.cangjie.cfir.resolve.calls.candidate

import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessibilityResult
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.CfirCallableLookupProvenance
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind

/**
 * tower discovery 产生的不可变 callable 描述。
 *
 * 描述只保留重新创建候选所需的声明与 receiver 结构，不携带旧 Candidate 的约束系统、
 * stage 进度、诊断或实参 replacement。[deterministicReturnType] 仅在返回类型已经完全确定时
 * 保存；为空表示 expected-return 细化必须回到完整 tower resolver。
 */
data class CfirCallableCandidateDiscovery(
    /** 被发现的 callable 符号。 */
    val symbol: CfirCallableSymbol<*>,
    /** discovery 时的 dispatch receiver 表达式。 */
    val dispatchReceiverExpression: CfirExpression?,
    /** discovery 时给出的 extension receiver 表达式。 */
    val givenExtensionReceiverExpression: CfirExpression?,
    /** 显式 receiver 的来源种类。 */
    val explicitReceiverKind: ExplicitReceiverKind,
    /** 产生该声明的 tower scope。 */
    val originScope: CfirScope?,
    /** 名字发现阶段计算出的语言级可访问性结果。 */
    val accessibilityResult: CfirAccessibilityResult?,
    /** effective member graph 的 extend/interface 来源。 */
    val lookupProvenance: CfirCallableLookupProvenance,
    /** 是否来自 companion object 类型 scope。 */
    val isFromCompanionObjectTypeScope: Boolean,
    /** smart-cast receiver 下是否来自原始类型。 */
    val isFromOriginalTypeInPresenceOfSmartCast: Boolean,
    /** 已应用候选 substitutor 且不含未固定分量的返回类型。 */
    val deterministicReturnType: ConeCangJieType?,
)
