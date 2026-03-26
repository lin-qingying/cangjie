

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.references.impl

import org.cangnova.cangjie.cfir.references.CfirResolvedErrorReference
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

internal class CfirResolvedErrorReferenceImpl(
    override val source: CjSourceElement?,
    override val name: Name,
    override val resolvedSymbol: CfirSymbol<*>,
    override val diagnostic: ConeDiagnostic,
) : CfirResolvedErrorReference() {

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirResolvedErrorReferenceImpl {
        return this
    }
}
