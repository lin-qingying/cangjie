package org.cangjie.cfir.expressions

import org.cangjie.cfir.common.CfirSourceElement
import org.cangjie.cfir.declarations.CfirFunction
import org.cangjie.cfir.types.ConeCangjieType
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

/**
 * Lambda 表达式，对应仓颉编译器中的 LambdaExpr。
 */
class CfirLambdaExpression(
    override val source: CfirSourceElement? = null,
    /** Lambda 对应的匿名函数声明 */
    val anonymousFunction: CfirFunction,
) : CfirExpression() {
    override var coneTypeOrNull: ConeCangjieType? = null

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitLambdaExpression(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirLambdaExpression = this
}
