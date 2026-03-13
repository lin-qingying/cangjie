

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.expressions.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirStringInterpolation
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor

class CfirStringInterpolationImpl @CfirImplementationDetail constructor(
    override var coneTypeOrNull: ConeCangjieType?,
    override var parts: List<CfirExpression>,
) : CfirStringInterpolation() {
    override val source: CjSourceElement?
        get() = null

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        parts.forEach { it.accept(visitor, data) }
    }

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangjieType?)
     {
        this.coneTypeOrNull = newConeTypeOrNull
    }

    override fun <D> transformParts(transformer: CfirTransformer<D>, data: D): CfirStringInterpolation
     {
        this.parts = parts.map { it.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirExpression }
        return this
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirStringInterpolationImpl {
        transformParts(transformer, data)
        return this
    }
}
