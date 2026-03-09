package org.cangjie.cfir.expressions

import org.cangjie.cfir.common.CfirSourceElement
import org.cangjie.cfir.types.ConeCangjieType
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

/**
 * 跳转类型。
 */
enum class CfirJumpKind {
    BREAK,
    CONTINUE,
}

/**
 * return 表达式。
 */
class CfirReturnExpression(
    override val source: CfirSourceElement? = null,
    var result: CfirExpression? = null,
) : CfirExpression() {
    override var coneTypeOrNull: ConeCangjieType? = null

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitReturnExpression(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirReturnExpression {
        result = result?.let { it.accept(transformer, data) as CfirExpression }
        return this
    }
}

/**
 * break / continue 跳转表达式。
 */
class CfirJumpExpression(
    override val source: CfirSourceElement? = null,
    val kind: CfirJumpKind,
) : CfirExpression() {
    override var coneTypeOrNull: ConeCangjieType? = null

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitJumpExpression(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirJumpExpression = this
}
