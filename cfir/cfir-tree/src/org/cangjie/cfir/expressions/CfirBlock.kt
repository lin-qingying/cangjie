package org.cangjie.cfir.expressions

import org.cangjie.cfir.CfirElement
import org.cangjie.cfir.common.CfirSourceElement
import org.cangjie.cfir.types.ConeCangjieType
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

/**
 * 代码块，包含一系列语句。
 */
class CfirBlock(
    override val source: CfirSourceElement? = null,
    val statements: MutableList<CfirStatement> = mutableListOf(),
) : CfirExpression() {
    override var coneTypeOrNull: ConeCangjieType? = null

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitBlock(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirBlock {
        statements.replaceAll { it.accept(transformer, data) as CfirStatement }
        return this
    }
}
