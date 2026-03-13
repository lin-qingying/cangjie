

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.types.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.types.CfirTupleTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor

class CfirTupleTypeRefImpl @CfirImplementationDetail constructor(
    override var elementTypeRefs: List<CfirTypeRef>,
) : CfirTupleTypeRef() {
    override val source: CjSourceElement?
        get() = null

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        elementTypeRefs.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformElementTypeRefs(transformer: CfirTransformer<D>, data: D): CfirTupleTypeRef
     {
        this.elementTypeRefs = elementTypeRefs.map { it.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirTypeRef }
        return this
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirTupleTypeRefImpl {
        transformElementTypeRefs(transformer, data)
        return this
    }
}
