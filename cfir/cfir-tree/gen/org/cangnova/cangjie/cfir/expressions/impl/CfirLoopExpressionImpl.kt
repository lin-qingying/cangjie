

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.cangjie.cfir.expressions.impl

import org.cangjie.cfir.CfirImplementationDetail
import org.cangjie.cfir.common.CfirSourceElement
import org.cangjie.cfir.expressions.CfirBlock
import org.cangjie.cfir.expressions.CfirExpression
import org.cangjie.cfir.expressions.CfirLoopExpression
import org.cangjie.cfir.types.ConeCangjieType
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

class CfirLoopExpressionImpl @CfirImplementationDetail constructor(
    override val coneTypeOrNull: ConeCangjieType?,
    override val condition: CfirExpression,
    override val body: CfirBlock,
    override val isDoWhile: Boolean,
) : CfirLoopExpression() {
    override val source: CfirSourceElement?
        get() = null

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        condition.accept(visitor, data)
        body.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirLoopExpressionImpl {
        condition.transform<org.cangjie.cfir.CfirElement, D>(transformer, data)
        body.transform<org.cangjie.cfir.CfirElement, D>(transformer, data)
        return this
    }
}
