package org.cangjie.cfir.expressions

import org.cangjie.cfir.common.CfirSourceElement
import org.cangjie.cfir.declarations.CfirVariable
import org.cangjie.cfir.types.ConeCangjieType
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

/**
 * while/do-while 循环，对应仓颉编译器中的 WhileExpr / DoWhileExpr。
 */
class CfirLoopExpression(
    override val source: CfirSourceElement? = null,
    var condition: CfirExpression,
    var body: CfirBlock,
    /** true = do-while, false = while */
    val isDoWhile: Boolean = false,
) : CfirExpression() {
    override var coneTypeOrNull: ConeCangjieType? = null

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitLoopExpression(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirLoopExpression {
        condition = condition.accept(transformer, data) as CfirExpression
        body = body.accept(transformer, data) as CfirBlock
        return this
    }
}

/**
 * for-in 循环，对应仓颉编译器中的 ForInExpr。
 */
class CfirForInExpression(
    override val source: CfirSourceElement? = null,
    /** 循环变量 */
    val variable: CfirVariable,
    /** 迭代源 */
    var iterable: CfirExpression,
    var body: CfirBlock,
) : CfirExpression() {
    override var coneTypeOrNull: ConeCangjieType? = null

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitForInExpression(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirForInExpression {
        iterable = iterable.accept(transformer, data) as CfirExpression
        body = body.accept(transformer, data) as CfirBlock
        return this
    }
}
