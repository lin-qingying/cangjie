

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.expressions.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirMatchBranch
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor

class CfirMatchExpressionImpl @CfirImplementationDetail constructor(
    override var coneTypeOrNull: ConeCangjieType?,
    override var subject: CfirExpression,
    override var branches: List<CfirMatchBranch>,
) : CfirMatchExpression() {
    override val source: CjSourceElement?
        get() = null

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        subject.accept(visitor, data)
        branches.forEach { it.accept(visitor, data) }
    }

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangjieType?)
     {
        this.coneTypeOrNull = newConeTypeOrNull
    }

    override fun <D> transformSubject(transformer: CfirTransformer<D>, data: D): CfirMatchExpression
     {
        this.subject = subject.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirExpression
        return this
    }

    override fun <D> transformBranches(transformer: CfirTransformer<D>, data: D): CfirMatchExpression
     {
        this.branches = branches.map { it.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirMatchBranch }
        return this
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirMatchExpressionImpl {
        transformSubject(transformer, data)
        transformBranches(transformer, data)
        return this
    }
}
