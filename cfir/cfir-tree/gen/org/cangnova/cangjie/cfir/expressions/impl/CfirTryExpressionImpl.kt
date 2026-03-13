

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.expressions.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirCatch
import org.cangnova.cangjie.cfir.expressions.CfirTryExpression
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor

class CfirTryExpressionImpl @CfirImplementationDetail constructor(
    override var coneTypeOrNull: ConeCangjieType?,
    override var tryBlock: CfirBlock,
    override var catches: List<CfirCatch>,
    override var finallyBlock: CfirBlock?,
) : CfirTryExpression() {
    override val source: CjSourceElement?
        get() = null

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        tryBlock.accept(visitor, data)
        catches.forEach { it.accept(visitor, data) }
        finallyBlock?.accept(visitor, data)
    }

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangjieType?)
     {
        this.coneTypeOrNull = newConeTypeOrNull
    }

    override fun <D> transformTryBlock(transformer: CfirTransformer<D>, data: D): CfirTryExpression
     {
        this.tryBlock = tryBlock.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirBlock
        return this
    }

    override fun <D> transformCatches(transformer: CfirTransformer<D>, data: D): CfirTryExpression
     {
        this.catches = catches.map { it.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirCatch }
        return this
    }

    override fun <D> transformFinallyBlock(transformer: CfirTransformer<D>, data: D): CfirTryExpression
     {
        this.finallyBlock = finallyBlock?.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirBlock?
        return this
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirTryExpressionImpl {
        transformTryBlock(transformer, data)
        transformCatches(transformer, data)
        transformFinallyBlock(transformer, data)
        return this
    }
}
