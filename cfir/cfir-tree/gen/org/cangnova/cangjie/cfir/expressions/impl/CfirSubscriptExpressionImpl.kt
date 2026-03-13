

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.expressions.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirSubscriptExpression
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor

class CfirSubscriptExpressionImpl @CfirImplementationDetail constructor(
    override var coneTypeOrNull: ConeCangjieType?,
    override var receiver: CfirExpression,
    override var indices: List<CfirExpression>,
) : CfirSubscriptExpression() {
    override val source: CjSourceElement?
        get() = null

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        receiver.accept(visitor, data)
        indices.forEach { it.accept(visitor, data) }
    }

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangjieType?)
     {
        this.coneTypeOrNull = newConeTypeOrNull
    }

    override fun <D> transformReceiver(transformer: CfirTransformer<D>, data: D): CfirSubscriptExpression
     {
        this.receiver = receiver.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirExpression
        return this
    }

    override fun <D> transformIndices(transformer: CfirTransformer<D>, data: D): CfirSubscriptExpression
     {
        this.indices = indices.map { it.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirExpression }
        return this
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirSubscriptExpressionImpl {
        transformReceiver(transformer, data)
        transformIndices(transformer, data)
        return this
    }
}
