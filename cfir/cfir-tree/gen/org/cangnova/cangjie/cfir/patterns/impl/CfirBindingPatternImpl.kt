

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.patterns.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.patterns.CfirBindingPattern
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

class CfirBindingPatternImpl @CfirImplementationDetail constructor(
    override val source: CjSourceElement?,
    override val name: Name,
    override var typeRef: CfirTypeRef?,
    override var nestedPattern: CfirPattern?,
) : CfirBindingPattern() {

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        typeRef?.accept(visitor, data)
        nestedPattern?.accept(visitor, data)
    }

    override fun <D> transformTypeRef(transformer: CfirTransformer<D>, data: D): CfirBindingPattern
     {
        this.typeRef = typeRef?.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirTypeRef?
        return this
    }

    override fun <D> transformNestedPattern(transformer: CfirTransformer<D>, data: D): CfirBindingPattern
     {
        this.nestedPattern = nestedPattern?.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirPattern?
        return this
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirBindingPatternImpl {
        transformTypeRef(transformer, data)
        transformNestedPattern(transformer, data)
        return this
    }
}
