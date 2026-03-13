

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.expressions.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.expressions.CfirBinaryOp
import org.cangnova.cangjie.cfir.expressions.CfirBinaryOpKind
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor

class CfirBinaryOpImpl @CfirImplementationDetail constructor(
    override var coneTypeOrNull: ConeCangjieType?,
    override val kind: CfirBinaryOpKind,
    override var left: CfirExpression,
    override var right: CfirExpression,
) : CfirBinaryOp() {
    override val source: CjSourceElement?
        get() = null

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        left.accept(visitor, data)
        right.accept(visitor, data)
    }

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangjieType?)
     {
        this.coneTypeOrNull = newConeTypeOrNull
    }

    override fun <D> transformLeft(transformer: CfirTransformer<D>, data: D): CfirBinaryOp
     {
        this.left = left.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirExpression
        return this
    }

    override fun <D> transformRight(transformer: CfirTransformer<D>, data: D): CfirBinaryOp
     {
        this.right = right.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirExpression
        return this
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirBinaryOpImpl {
        transformLeft(transformer, data)
        transformRight(transformer, data)
        return this
    }
}
