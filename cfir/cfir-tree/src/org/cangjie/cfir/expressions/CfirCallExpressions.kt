package org.cangjie.cfir.expressions

import org.cangjie.cfir.common.CfirSourceElement
import org.cangjie.cfir.references.CfirReference
import org.cangjie.cfir.types.CfirTypeRef
import org.cangjie.cfir.types.ConeCangjieType
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

/**
 * 函数调用表达式，对应仓颉编译器中的 CallExpr。
 */
class CfirFunctionCall(
    override val source: CfirSourceElement? = null,
    /** 被调用的函数引用 */
    var calleeReference: CfirReference,
    /** 显式接收者（成员调用时的左侧对象） */
    var explicitReceiver: CfirExpression? = null,
    /** 参数列表 */
    val arguments: MutableList<CfirExpression> = mutableListOf(),
    /** 泛型类型实参 */
    val typeArguments: List<CfirTypeRef> = emptyList(),
) : CfirExpression() {
    override var coneTypeOrNull: ConeCangjieType? = null

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitFunctionCall(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirFunctionCall {
        explicitReceiver = explicitReceiver?.let { it.accept(transformer, data) as CfirExpression }
        arguments.replaceAll { it.accept(transformer, data) as CfirExpression }
        return this
    }
}

/**
 * 属性访问表达式，对应仓颉编译器中的 MemberAccessExpr（属性访问部分）。
 */
class CfirPropertyAccess(
    override val source: CfirSourceElement? = null,
    var calleeReference: CfirReference,
    var explicitReceiver: CfirExpression? = null,
) : CfirExpression() {
    override var coneTypeOrNull: ConeCangjieType? = null

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitPropertyAccess(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirPropertyAccess {
        explicitReceiver = explicitReceiver?.let { it.accept(transformer, data) as CfirExpression }
        return this
    }
}

/**
 * 限定访问表达式（通用的成员访问）。
 */
class CfirQualifiedAccess(
    override val source: CfirSourceElement? = null,
    var calleeReference: CfirReference,
    var explicitReceiver: CfirExpression? = null,
    val typeArguments: List<CfirTypeRef> = emptyList(),
) : CfirExpression() {
    override var coneTypeOrNull: ConeCangjieType? = null

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitQualifiedAccess(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirQualifiedAccess {
        explicitReceiver = explicitReceiver?.let { it.accept(transformer, data) as CfirExpression }
        return this
    }
}
