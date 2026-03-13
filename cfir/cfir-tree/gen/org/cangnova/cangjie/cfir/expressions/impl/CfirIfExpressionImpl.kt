

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.cangjie.cfir.expressions.impl

import org.cangjie.cfir.CfirImplementationDetail
import org.cangjie.cfir.common.CfirSourceElement
import org.cangjie.cfir.expressions.CfirBlock
import org.cangjie.cfir.expressions.CfirExpression
import org.cangjie.cfir.expressions.CfirIfExpression
import org.cangjie.cfir.types.ConeCangjieType
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

class CfirIfExpressionImpl @CfirImplementationDetail constructor(
    override val coneTypeOrNull: ConeCangjieType?,
    override val condition: CfirExpression,
    override val thenBranch: CfirBlock,
    override val elseBranch: CfirExpression?,
) : CfirIfExpression() {
    override val source: CfirSourceElement?
        get() = null

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        condition.accept(visitor, data)
        thenBranch.accept(visitor, data)
        elseBranch?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirIfExpressionImpl {
        condition.transform<org.cangjie.cfir.CfirElement, D>(transformer, data)
        thenBranch.transform<org.cangjie.cfir.CfirElement, D>(transformer, data)
        elseBranch?.transform<org.cangjie.cfir.CfirElement, D>(transformer, data)
        return this
    }
}
