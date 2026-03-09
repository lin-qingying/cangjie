package org.cangjie.cfir.expressions

import org.cangjie.cfir.common.CfirSourceElement
import org.cangjie.cfir.declarations.CfirValueParameter
import org.cangjie.cfir.types.CfirTypeRef
import org.cangjie.cfir.types.ConeCangjieType
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

/**
 * try-catch-finally 表达式，对应仓颉编译器中的 TryExpr。
 */
class CfirTryExpression(
    override val source: CfirSourceElement? = null,
    var tryBlock: CfirBlock,
    val catches: MutableList<CfirCatch> = mutableListOf(),
    var finallyBlock: CfirBlock? = null,
) : CfirExpression() {
    override var coneTypeOrNull: ConeCangjieType? = null

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitTryExpression(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirTryExpression {
        tryBlock = tryBlock.accept(transformer, data) as CfirBlock
        catches.replaceAll { it.accept(transformer, data) as CfirCatch }
        finallyBlock = finallyBlock?.let { it.accept(transformer, data) as CfirBlock }
        return this
    }
}

/**
 * catch 子句。
 */
class CfirCatch(
    override val source: CfirSourceElement? = null,
    /** 捕获的异常参数 */
    val parameter: CfirValueParameter,
    var body: CfirBlock,
) : CfirExpression() {
    override var coneTypeOrNull: ConeCangjieType? = null

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitElement(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirCatch {
        body = body.accept(transformer, data) as CfirBlock
        return this
    }
}

/**
 * throw 表达式，对应仓颉编译器中的 ThrowExpr。
 */
class CfirThrowExpression(
    override val source: CfirSourceElement? = null,
    var exception: CfirExpression,
) : CfirExpression() {
    override var coneTypeOrNull: ConeCangjieType? = null

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitThrowExpression(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirThrowExpression {
        exception = exception.accept(transformer, data) as CfirExpression
        return this
    }
}
