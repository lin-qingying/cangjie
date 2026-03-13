

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.tupleTypeRef]
 */
abstract class CfirTupleTypeRef : CfirTypeRef() {
    abstract override val source: CjSourceElement?
    abstract var elementTypeRefs: List<CfirTypeRef>

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitTupleTypeRef(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformTupleTypeRef(this, data) as E

    abstract fun <D> transformElementTypeRefs(transformer: CfirTransformer<D>, data: D): CfirTupleTypeRef

}
