

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.expressions.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirMatchBranch
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor

class CfirMatchBranchImpl @CfirImplementationDetail constructor(
    override var coneTypeOrNull: ConeCangjieType?,
    override var pattern: CfirPattern,
    override var guard: CfirExpression?,
    override var body: CfirBlock,
) : CfirMatchBranch() {
    override val source: CjSourceElement?
        get() = null

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        pattern.accept(visitor, data)
        guard?.accept(visitor, data)
        body.accept(visitor, data)
    }

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangjieType?)
     {
        this.coneTypeOrNull = newConeTypeOrNull
    }

    override fun <D> transformPattern(transformer: CfirTransformer<D>, data: D): CfirMatchBranch
     {
        this.pattern = pattern.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirPattern
        return this
    }

    override fun <D> transformGuard(transformer: CfirTransformer<D>, data: D): CfirMatchBranch
     {
        this.guard = guard?.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirExpression?
        return this
    }

    override fun <D> transformBody(transformer: CfirTransformer<D>, data: D): CfirMatchBranch
     {
        this.body = body.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirBlock
        return this
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirMatchBranchImpl {
        transformPattern(transformer, data)
        transformGuard(transformer, data)
        transformBody(transformer, data)
        return this
    }
}
