

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.references.impl

import org.cangnova.cangjie.cfir.references.CfirSuperReference
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

internal class CfirSuperReferenceImpl(
    override val source: CjSourceElement?,
    override var superTypeRef: CfirTypeRef,
) : CfirSuperReference() {

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        superTypeRef.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirSuperReferenceImpl {
        superTypeRef = superTypeRef.transform(transformer, data)
        return this
    }

    override fun replaceSuperTypeRef(newSuperTypeRef: CfirTypeRef) {
        superTypeRef = newSuperTypeRef
    }
}
