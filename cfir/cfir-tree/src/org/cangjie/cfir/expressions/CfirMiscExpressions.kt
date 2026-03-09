package org.cangjie.cfir.expressions

import org.cangjie.cfir.common.CfirSourceElement
import org.cangjie.cfir.types.ConeCangjieType
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

/**
 * 区间表达式（仓颉特有），对应仓颉编译器中的 RangeExpr。
 *
 * 支持 a..b, a..=b, a..<b 等区间形式。
 */
class CfirRangeExpression(
    override val source: CfirSourceElement? = null,
    var start: CfirExpression,
    var end: CfirExpression,
    /** 区间是否包含右端点 */
    val isInclusive: Boolean = false,
) : CfirExpression() {
    override var coneTypeOrNull: ConeCangjieType? = null

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitRangeExpression(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirRangeExpression {
        start = start.accept(transformer, data) as CfirExpression
        end = end.accept(transformer, data) as CfirExpression
        return this
    }
}

/**
 * 数组字面量表达式。
 */
class CfirArrayLiteral(
    override val source: CfirSourceElement? = null,
    val elements: MutableList<CfirExpression> = mutableListOf(),
) : CfirExpression() {
    override var coneTypeOrNull: ConeCangjieType? = null

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitArrayLiteral(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirArrayLiteral {
        elements.replaceAll { it.accept(transformer, data) as CfirExpression }
        return this
    }
}

/**
 * 元组字面量表达式。
 */
class CfirTupleLiteral(
    override val source: CfirSourceElement? = null,
    val elements: MutableList<CfirExpression> = mutableListOf(),
) : CfirExpression() {
    override var coneTypeOrNull: ConeCangjieType? = null

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitTupleLiteral(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirTupleLiteral {
        elements.replaceAll { it.accept(transformer, data) as CfirExpression }
        return this
    }
}

/**
 * spawn 表达式（仓颉并发），对应仓颉编译器中的 SpawnExpr。
 */
class CfirSpawnExpression(
    override val source: CfirSourceElement? = null,
    var body: CfirBlock,
) : CfirExpression() {
    override var coneTypeOrNull: ConeCangjieType? = null

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitSpawnExpression(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirSpawnExpression {
        body = body.accept(transformer, data) as CfirBlock
        return this
    }
}

/**
 * 下标表达式（索引访问），对应仓颉编译器中的 SubscriptExpr。
 */
class CfirSubscriptExpression(
    override val source: CfirSourceElement? = null,
    var receiver: CfirExpression,
    val indices: MutableList<CfirExpression> = mutableListOf(),
) : CfirExpression() {
    override var coneTypeOrNull: ConeCangjieType? = null

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitSubscriptExpression(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirSubscriptExpression {
        receiver = receiver.accept(transformer, data) as CfirExpression
        indices.replaceAll { it.accept(transformer, data) as CfirExpression }
        return this
    }
}

/**
 * 错误表达式（解析失败时的占位符）。
 */
class CfirErrorExpression(
    override val source: CfirSourceElement? = null,
    val reason: String,
) : CfirExpression() {
    override var coneTypeOrNull: ConeCangjieType? = null

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitErrorExpression(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirErrorExpression = this
}
