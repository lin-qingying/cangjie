

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.references.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.references.CfirResolvedBuiltinOperatorReference
import org.cangnova.cangjie.cfir.types.BuiltinPrimitiveOperatorSignature
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

class CfirResolvedBuiltinOperatorReferenceImpl @CfirImplementationDetail constructor(
    override var source: CjSourceElement?,
    override val name: Name,
    override val signature: BuiltinPrimitiveOperatorSignature,
) : CfirResolvedBuiltinOperatorReference() {

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirResolvedBuiltinOperatorReferenceImpl {
        return this
    }

    @CfirImplementationDetail
    override fun replaceSource(newSource: CjSourceElement?) {
        source = newSource
    }
}
