package org.cangjie.cfir.expressions

import org.cangjie.cfir.common.CfirSourceElement
import org.cangjie.cfir.patterns.CfirPattern
import org.cangjie.cfir.types.ConeCangjieType
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

/**
 * if 表达式，对应仓颉编译器中的 IfExpr。
 *
 * 在仓颉中 if 是表达式，可以有返回值。
 */
class CfirIfExpression(
    override val source: CfirSourceElement? = null,
    var condition: CfirExpression,
    var thenBranch: CfirBlock,
    var elseBranch: CfirExpression? = null,
) : CfirExpression() {
    override var coneTypeOrNull: ConeCangjieType? = null

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitIfExpression(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirIfExpression {
        condition = condition.accept(transformer, data) as CfirExpression
        thenBranch = thenBranch.accept(transformer, data) as CfirBlock
        elseBranch = elseBranch?.let { it.accept(transformer, data) as CfirExpression }
        return this
    }
}

/**
 * match 表达式（仓颉特有），对应仓颉编译器中的 MatchExpr。
 *
 * 仓颉的 match 是功能丰富的模式匹配表达式。
 */
class CfirMatchExpression(
    override val source: CfirSourceElement? = null,
    var subject: CfirExpression,
    val branches: MutableList<CfirMatchBranch> = mutableListOf(),
) : CfirExpression() {
    override var coneTypeOrNull: ConeCangjieType? = null

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitMatchExpression(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirMatchExpression {
        subject = subject.accept(transformer, data) as CfirExpression
        branches.replaceAll { it.accept(transformer, data) as CfirMatchBranch }
        return this
    }
}

/**
 * match 分支。
 */
class CfirMatchBranch(
    override val source: CfirSourceElement? = null,
    /** 匹配模式 */
    val pattern: CfirPattern,
    /** 守卫条件（where 子句） */
    var guard: CfirExpression? = null,
    /** 分支体 */
    var body: CfirBlock,
) : CfirExpression() {
    override var coneTypeOrNull: ConeCangjieType? = null

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitMatchBranch(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirMatchBranch {
        guard = guard?.let { it.accept(transformer, data) as CfirExpression }
        body = body.accept(transformer, data) as CfirBlock
        return this
    }
}
