

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.patterns.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.patterns.CfirTypePattern
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.name.Name

class CfirTypePatternImpl @CfirImplementationDetail constructor(
    override val source: CjSourceElement?,
    override var typeRef: CfirTypeRef,
    override val bindingName: Name?,
) : CfirTypePattern() {

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        typeRef.accept(visitor, data)
    }

    override fun <D> transformTypeRef(transformer: CfirTransformer<D>, data: D): CfirTypePattern
     {
        this.typeRef = typeRef.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirTypeRef
        return this
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirTypePatternImpl {
        transformTypeRef(transformer, data)
        return this
    }
}
