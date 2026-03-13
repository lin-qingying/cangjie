

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.expressions.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirIfExpression
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor

class CfirIfExpressionImpl @CfirImplementationDetail constructor(
    override var coneTypeOrNull: ConeCangjieType?,
    override var condition: CfirExpression,
    override var thenBranch: CfirBlock,
    override var elseBranch: CfirExpression?,
) : CfirIfExpression() {
    override val source: CjSourceElement?
        get() = null

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        condition.accept(visitor, data)
        thenBranch.accept(visitor, data)
        elseBranch?.accept(visitor, data)
    }

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangjieType?)
     {
        this.coneTypeOrNull = newConeTypeOrNull
    }

    override fun <D> transformCondition(transformer: CfirTransformer<D>, data: D): CfirIfExpression
     {
        this.condition = condition.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirExpression
        return this
    }

    override fun <D> transformThenBranch(transformer: CfirTransformer<D>, data: D): CfirIfExpression
     {
        this.thenBranch = thenBranch.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirBlock
        return this
    }

    override fun <D> transformElseBranch(transformer: CfirTransformer<D>, data: D): CfirIfExpression
     {
        this.elseBranch = elseBranch?.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirExpression?
        return this
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirIfExpressionImpl {
        transformCondition(transformer, data)
        transformThenBranch(transformer, data)
        transformElseBranch(transformer, data)
        return this
    }
}
