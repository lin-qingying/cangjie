package org.cangnova.cangjie.cfir.expressions

import org.cangnova.cangjie.cfir.CfirElement

import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * 取得当前 CFIR 元素对应的引用节点。
 *
 * 解析在 use-site [session] 上下文中执行；某些基于 ID 的表达式可能需要会话才能恢复符号引用。
 */
fun CfirElement.toReference(session: CfirSession): CfirReference? {
    return when (this) {
        is CfirExpression -> toReferenceImpl(session)
//        is CfirVariableAssignment -> calleeReference
        is CfirResolvable -> calleeReference
        else -> null
    }
}

/**
 * 从表达式节点递归提取底层引用。
 *
 * smart cast 等包装表达式不拥有独立 callee，需要继续回到原始表达式；普通可解析表达式直接返回
 * 自身的 [CfirResolvable.calleeReference]。
 */
private fun CfirExpression.toReferenceImpl(session: CfirSession?): CfirReference? {
    return when (this) {
//        is CfirEnumEntryDeserializedAccessExpression -> {
//            requireNotNull(session)
//            toReference(session)
//        }
//        is CfirWrappedArgumentExpression -> expression.toResolvedCallableReferenceImpl(session)
        is CfirSmartCastExpression -> originalExpression.toReferenceImpl(session)
//        is CfirDesugaredAssignmentValueReferenceExpression -> expressionRef.value.toReferenceImpl(session)
        is CfirResolvable -> calleeReference
        else -> null
    }
}
