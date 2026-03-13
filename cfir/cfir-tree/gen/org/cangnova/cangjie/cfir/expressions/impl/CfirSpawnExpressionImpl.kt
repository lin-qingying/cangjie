

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.expressions.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirSpawnExpression
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor

class CfirSpawnExpressionImpl @CfirImplementationDetail constructor(
    override var coneTypeOrNull: ConeCangjieType?,
    override var body: CfirBlock,
) : CfirSpawnExpression() {
    override val source: CjSourceElement?
        get() = null

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        body.accept(visitor, data)
    }

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangjieType?)
     {
        this.coneTypeOrNull = newConeTypeOrNull
    }

    override fun <D> transformBody(transformer: CfirTransformer<D>, data: D): CfirSpawnExpression
     {
        this.body = body.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirBlock
        return this
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirSpawnExpressionImpl {
        transformBody(transformer, data)
        return this
    }
}
