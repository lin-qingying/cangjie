

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.expressions.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor

class CfirAssignmentImpl @CfirImplementationDetail constructor(
    override var coneTypeOrNull: ConeCangjieType?,
    override var lValue: CfirExpression,
    override var rValue: CfirExpression,
) : CfirAssignment() {
    override val source: CjSourceElement?
        get() = null

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        lValue.accept(visitor, data)
        rValue.accept(visitor, data)
    }

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangjieType?)
     {
        this.coneTypeOrNull = newConeTypeOrNull
    }

    override fun <D> transformLValue(transformer: CfirTransformer<D>, data: D): CfirAssignment
     {
        this.lValue = lValue.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirExpression
        return this
    }

    override fun <D> transformRValue(transformer: CfirTransformer<D>, data: D): CfirAssignment
     {
        this.rValue = rValue.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirExpression
        return this
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirAssignmentImpl {
        transformLValue(transformer, data)
        transformRValue(transformer, data)
        return this
    }
}
