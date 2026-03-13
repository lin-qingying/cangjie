

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.patterns.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.patterns.CfirTuplePattern
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor

class CfirTuplePatternImpl @CfirImplementationDetail constructor(
    override var elements: List<CfirPattern>,
) : CfirTuplePattern() {
    override val source: CjSourceElement?
        get() = null

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
