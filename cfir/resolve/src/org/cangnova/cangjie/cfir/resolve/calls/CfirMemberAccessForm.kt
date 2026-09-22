package org.cangnova.cangjie.cfir.resolve.calls

import org.cangnova.cangjie.cfir.calls.qualifierScopeOrNull
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCallOrigin
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallInfo
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions

/**
 * 成员访问的**形态**与成员声明 `static` 属性不一致时官方报告的专用错误。
 *
 * 官方在成员发现阶段（`TypeCheckReference.cpp` 的 `FilterAndGetTargetsOfObjAccess` /
 * `FilterAndCheckTargetsOfNameAccess`）就按 `Attribute::STATIC` 过滤目标集，目标集为空即报告
 * 专用诊断并直接返回，**调用重载解析根本不会运行**（`NameReferenceExpr.cpp` 的
 * `InferStaticAccess` / `GetObjMemberAccessTarget` 诊断后 return，`ma.target` 保持为空）。
 * 访问控制过滤 `GetAccessibleDecls` 位于形态过滤**之后**。因此形态判定必须先于
 * 可见性判定成立，见 [CfirAccessibilityChecker] 的处置模型与 `TowerLevelHandler` 的发现门。
 */
internal sealed interface CfirMemberAccessFormError {
    /** 被错误访问的成员名。 */
    val memberName: Name

    /** 对象接收者访问 static 成员（官方 `sema_object_cannot_access_static_member`）。 */
    data class ObjectAccessedStaticMember(
        override val memberName: Name,
    ) : CfirMemberAccessFormError

    /** 类型名访问实例成员（官方 `sema_illegal_access_non_static_member`）。 */
    data class TypeQualifierAccessedNonStaticMember(
        override val memberName: Name,
    ) : CfirMemberAccessFormError
}

/**
 * 判定一次成员访问是否违反「接收者形态 × 成员 static 属性」一致性。
 *
 * 官方判定只依赖语法接收者形态（`InferMemberAccess` 的 `isStaticAccessByName`）与被声明成员是否
 * 带 `STATIC` 属性，与可见性无关——因此 `A.l()`（private 实例成员）仍报
 * `sema_illegal_access_non_static_member`，`A().o()`（private static 成员）仍报
 * `sema_object_cannot_access_static_member`，而不是退化成调用 no-match。
 *
 * @param receiverExpression 源码中显式写出的接收者表达式；只有在同时存在显式接收者时才判定。
 */
internal fun memberAccessFormError(
    symbol: CfirCallableSymbol<*>,
    callInfo: CallInfo,
    receiverExpression: CfirExpression?,
    context: ResolutionContext,
): CfirMemberAccessFormError? {
    if (callInfo.explicitReceiver == null) return null
    val isTypeQualifierReceiver = receiverExpression.isTypeQualifierReceiver(context)
    if (symbol.cfir.status.isStatic) {
        return when {
            isTypeQualifierReceiver -> null
            receiverExpression == null -> null
            else -> CfirMemberAccessFormError.ObjectAccessedStaticMember(symbol.name)
        }
    }
    if (symbol is CfirEnumConstructorSymbol) return null
    if (!callInfo.isMemberSyntaxOrSubscriptAccess()) return null
    if (!isTypeQualifierReceiver) return null
    return CfirMemberAccessFormError.TypeQualifierAccessedNonStaticMember(symbol.name)
}

/**
 * 判断接收者表达式是否为 class / typealias / 内建类型 qualifier。
 *
 * qualifier 是名字查找的 base expression，不是运行时值接收者；它同时决定
 * 「类型名访问实例成员」与「static 成员的类型限定访问」两条判定分支。
 */
internal fun CfirExpression?.isTypeQualifierReceiver(context: ResolutionContext): Boolean {
    val expression = this ?: return false
    return expression.qualifierScopeOrNull(context.session, context.bodyResolveComponents.scopeSession) != null
}

/**
 * 官方只在成员访问语法和下标 get/set 上报告「类型名访问实例成员」。
 *
 * 普通二元/一元操作符里的裸类型名仍按表达式位置处理为 `REF_NOT_BE_TYPE`，
 * 不能因为候选里有同名实例 operator 就改报非静态成员访问。
 */
internal fun CallInfo.isMemberSyntaxOrSubscriptAccess(): Boolean {
    if (origin != CfirFunctionCallOrigin.Operator) return true
    return name == OperatorNameConventions.GET || name == OperatorNameConventions.SET
}
