

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.cangjie.cfir.expressions.impl

import org.cangjie.cfir.CfirImplementationDetail
import org.cangjie.cfir.common.CfirSourceElement
import org.cangjie.cfir.declarations.CfirVariable
import org.cangjie.cfir.expressions.CfirBlock
import org.cangjie.cfir.expressions.CfirExpression
import org.cangjie.cfir.expressions.CfirForInExpression
import org.cangjie.cfir.types.ConeCangjieType
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

class CfirForInExpressionImpl @CfirImplementationDetail constructor(
    override val coneTypeOrNull: ConeCangjieType?,
    override val condition: CfirExpression,
    override val isDoWhile: Boolean,
    override val variable: CfirVariable,
    override val iterable: CfirExpression,
    override val body: CfirBlock,
) : CfirForInExpression() {
    override val source: CfirSourceElement?
        get() = null

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        condition.accept(visitor, data)
        variable.accept(visitor, data)
        iterable.accept(visitor, data)
        body.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirForInExpressionImpl {
        condition.transform<org.cangjie.cfir.CfirElement, D>(transformer, data)
        variable.transform<org.cangjie.cfir.CfirElement, D>(transformer, data)
        iterable.transform<org.cangjie.cfir.CfirElement, D>(transformer, data)
        body.transform<org.cangjie.cfir.CfirElement, D>(transformer, data)
        return this
    }
}
