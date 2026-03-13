

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.types.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.CfirVArrayTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor

class CfirVArrayTypeRefImpl @CfirImplementationDetail constructor(
    override var elementTypeRef: CfirTypeRef,
    override val sizeLiteral: String,
) : CfirVArrayTypeRef() {
    override val source: CjSourceElement?
        get() = null

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        elementTypeRef.accept(visitor, data)
    }

    override fun <D> transformElementTypeRef(transformer: CfirTransformer<D>, data: D): CfirVArrayTypeRef
     {
        this.elementTypeRef = elementTypeRef.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirTypeRef
        return this
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirVArrayTypeRefImpl {
        transformElementTypeRef(transformer, data)
        return this
    }
}
