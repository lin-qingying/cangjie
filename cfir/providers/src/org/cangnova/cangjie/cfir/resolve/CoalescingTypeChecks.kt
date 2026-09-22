package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeStubType
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType
import org.cangnova.cangjie.cfir.types.contains
import org.cangnova.cangjie.cfir.types.containsErrorType
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * 官方 `ChkCoalescingExpr`（`Sema/TypeCheckExpr/BinaryExpr.cpp:1127-1135`）的 `realTgtTy`：
 * 有上下文目标类型且左操作数元素类型可作为该目标类型时使用上下文目标类型，否则使用元素类型。
 *
 * resolve（下推右操作数期望类型、决定 `??` 结果类型）与 checker（在右操作数上报告不匹配）
 * 消费的是同一个目标类型，只在此处定义一次。
 */
fun coalescingTargetType(
    leftElementType: ConeCangJieType,
    contextualExpectedType: ConeCangJieType?,
    session: CfirSession,
): ConeCangJieType {
    if (contextualExpectedType == null) return leftElementType
    return if (AbstractTypeChecker.isSubtypeOf(session.typeContext, leftElementType, contextualExpectedType) == true) {
        contextualExpectedType
    } else {
        leftElementType
    }
}

/**
 * 判断 `??` 的右操作数是否**确定**不满足 [targetType]。
 *
 * 对应官方 `ChkCoalescingExpr` 的右操作数分支（`BinaryExpr.cpp:1136-1140`）：检查失败时整个
 * `??` 为 InvalidTy，诊断锚在右操作数上。官方 `DiagMismatchedTypes` 只在节点类型仍正确时才报告，
 * 因此该判定仅在两侧类型都已定型时成立——含类型变量、stub 或错误类型时留给推断/其它错误路径，
 * 不能据此判定不匹配（否则会把仍在推断中的类型误毒化）。
 */
fun isDefiniteCoalescingRightMismatch(
    rightType: ConeCangJieType,
    targetType: ConeCangJieType,
    session: CfirSession,
): Boolean {
    if (rightType.containsErrorType() || targetType.containsErrorType()) return false
    if (rightType.containsTypeVariableOrStub() || targetType.containsTypeVariableOrStub()) return false
    return AbstractTypeChecker.isSubtypeOf(session.typeContext, rightType, targetType) == false
}

/** 类型树中是否含有类型变量或 stub（尚未定型的推断中间态）。 */
private fun ConeCangJieType.containsTypeVariableOrStub(): Boolean =
    contains { it is ConeTypeVariableType || it is ConeStubType }
