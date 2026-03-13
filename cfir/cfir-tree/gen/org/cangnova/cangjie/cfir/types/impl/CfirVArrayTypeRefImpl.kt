

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.cangjie.cfir.types.impl

import org.cangjie.cfir.CfirImplementationDetail
import org.cangjie.cfir.common.CfirSourceElement
import org.cangjie.cfir.types.CfirTypeRef
import org.cangjie.cfir.types.CfirVArrayTypeRef
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

class CfirVArrayTypeRefImpl @CfirImplementationDetail constructor(
    override val elementTypeRef: CfirTypeRef,
    override val sizeLiteral: String,
) : CfirVArrayTypeRef() {
    override val source: CfirSourceElement?
        get() = null

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        elementTypeRef.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirVArrayTypeRefImpl {
        elementTypeRef.transform<org.cangjie.cfir.CfirElement, D>(transformer, data)
        return this
    }
}
