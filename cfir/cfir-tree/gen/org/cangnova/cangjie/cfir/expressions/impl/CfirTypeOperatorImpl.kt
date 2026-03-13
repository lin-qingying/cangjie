

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.expressions.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirTypeOperationKind
import org.cangnova.cangjie.cfir.expressions.CfirTypeOperator
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor

class CfirTypeOperatorImpl @CfirImplementationDetail constructor(
    override var coneTypeOrNull: ConeCangjieType?,
    override val operation: CfirTypeOperationKind,
    override var argument: CfirExpression,
    override var typeRef: CfirTypeRef,
) : CfirTypeOperator() {
    override val source: CjSourceElement?
        get() = null

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        argument.accept(visitor, data)
        typeRef.accept(visitor, data)
    }

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangjieType?)
     {
        this.coneTypeOrNull = newConeTypeOrNull
    }

    override fun <D> transformArgument(transformer: CfirTransformer<D>, data: D): CfirTypeOperator
     {
        this.argument = argument.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirExpression
        return this
    }

    override fun <D> transformTypeRef(transformer: CfirTransformer<D>, data: D): CfirTypeOperator
     {
        this.typeRef = typeRef.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirTypeRef
        return this
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirTypeOperatorImpl {
        transformArgument(transformer, data)
        transformTypeRef(transformer, data)
        return this
    }
}
