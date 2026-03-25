

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.references.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.references.CfirThisReference
import org.cangnova.cangjie.cfir.symbols.CfirThisOwnerSymbol
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

class CfirThisReferenceImpl @CfirImplementationDetail constructor(
    override val source: CjSourceElement?,
    override var boundSymbol: CfirThisOwnerSymbol<*>?,
    override val isImplicit: Boolean,
    override var diagnostic: ConeDiagnostic?,
) : CfirThisReference() {

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
    }

    override fun replaceBoundSymbol(newBoundSymbol: CfirThisOwnerSymbol<*>?)
     {
        this.boundSymbol = newBoundSymbol
    }

    override fun replaceDiagnostic(newDiagnostic: ConeDiagnostic?)
     {
        this.diagnostic = newDiagnostic
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirThisReferenceImpl {
        return this
    }
}
