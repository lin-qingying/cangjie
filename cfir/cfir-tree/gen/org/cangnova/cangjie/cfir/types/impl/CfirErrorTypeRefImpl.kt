

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.cangjie.cfir.types.impl

import org.cangjie.cfir.CfirImplementationDetail
import org.cangjie.cfir.common.CfirSourceElement
import org.cangjie.cfir.types.CfirErrorTypeRef
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

class CfirErrorTypeRefImpl @CfirImplementationDetail constructor(
    override val reason: String,
) : CfirErrorTypeRef() {
    override val source: CfirSourceElement?
        get() = null

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirErrorTypeRefImpl {
        return this
    }
}
