

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.patterns.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.patterns.CfirTuplePattern
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

class CfirTuplePatternImpl @CfirImplementationDetail constructor(
    override val source: CjSourceElement?,
    override var elements: List<CfirPattern>,
) : CfirTuplePattern() {

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        elements.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformElements(transformer: CfirTransformer<D>, data: D): CfirTuplePattern
     {
        this.elements = elements.map { it.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirPattern }
        return this
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirTuplePatternImpl {
        transformElements(transformer, data)
        return this
    }
}
