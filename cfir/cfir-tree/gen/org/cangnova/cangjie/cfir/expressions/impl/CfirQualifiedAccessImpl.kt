

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.cangjie.cfir.expressions.impl

import org.cangjie.cfir.CfirImplementationDetail
import org.cangjie.cfir.common.CfirSourceElement
import org.cangjie.cfir.expressions.CfirExpression
import org.cangjie.cfir.expressions.CfirQualifiedAccess
import org.cangjie.cfir.references.CfirReference
import org.cangjie.cfir.types.CfirTypeRef
import org.cangjie.cfir.types.ConeCangjieType
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

class CfirQualifiedAccessImpl @CfirImplementationDetail constructor(
    override val coneTypeOrNull: ConeCangjieType?,
    override val calleeReference: CfirReference,
    override val explicitReceiver: CfirExpression?,
    override val typeArguments: List<CfirTypeRef>,
) : CfirQualifiedAccess() {
    override val source: CfirSourceElement?
        get() = null

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        calleeReference.accept(visitor, data)
        explicitReceiver?.accept(visitor, data)
        typeArguments.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirQualifiedAccessImpl {
        calleeReference.transform<org.cangjie.cfir.CfirElement, D>(transformer, data)
        explicitReceiver?.transform<org.cangjie.cfir.CfirElement, D>(transformer, data)
        typeArguments.forEach { it.transform<org.cangjie.cfir.CfirElement, D>(transformer, data) }
        return this
    }
}
