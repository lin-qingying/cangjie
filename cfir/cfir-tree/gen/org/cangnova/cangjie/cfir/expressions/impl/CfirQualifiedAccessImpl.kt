

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.expressions.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccess
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor

class CfirQualifiedAccessImpl @CfirImplementationDetail constructor(
    override var coneTypeOrNull: ConeCangjieType?,
    override var calleeReference: CfirReference,
    override var explicitReceiver: CfirExpression?,
    override var typeArguments: List<CfirTypeRef>,
) : CfirQualifiedAccess() {
    override val source: CjSourceElement?
        get() = null

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        calleeReference.accept(visitor, data)
        explicitReceiver?.accept(visitor, data)
        typeArguments.forEach { it.accept(visitor, data) }
    }

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangjieType?)
     {
        this.coneTypeOrNull = newConeTypeOrNull
    }

    override fun <D> transformCalleeReference(transformer: CfirTransformer<D>, data: D): CfirQualifiedAccess
     {
        this.calleeReference = calleeReference.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirReference
        return this
    }

    override fun <D> transformExplicitReceiver(transformer: CfirTransformer<D>, data: D): CfirQualifiedAccess
     {
        this.explicitReceiver = explicitReceiver?.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirExpression?
        return this
    }

    override fun <D> transformTypeArguments(transformer: CfirTransformer<D>, data: D): CfirQualifiedAccess
     {
        this.typeArguments = typeArguments.map { it.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirTypeRef }
        return this
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirQualifiedAccessImpl {
        transformCalleeReference(transformer, data)
        transformExplicitReceiver(transformer, data)
        transformTypeArguments(transformer, data)
        return this
    }
}
