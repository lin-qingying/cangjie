

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.cangjie.cfir.patterns.impl

import org.cangjie.cfir.CfirImplementationDetail
import org.cangjie.cfir.common.CfirSourceElement
import org.cangjie.cfir.patterns.CfirPattern
import org.cangjie.cfir.patterns.CfirTuplePattern
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

class CfirTuplePatternImpl @CfirImplementationDetail constructor(
    override val elements: List<CfirPattern>,
) : CfirTuplePattern() {
    override val source: CfirSourceElement?
        get() = null

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        elements.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirTuplePatternImpl {
        elements.forEach { it.transform<org.cangjie.cfir.CfirElement, D>(transformer, data) }
        return this
    }
}
