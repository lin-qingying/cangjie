

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.cangjie.cfir.types

import org.cangjie.cfir.CfirElement
import org.cangjie.cfir.common.CfirSourceElement
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

/**
 * Generated from: [org.cangjie.cfir.tree.generator.CfirTree.varrayTypeRef]
 */
abstract class CfirVArrayTypeRef : CfirTypeRef() {
    abstract override val source: CfirSourceElement?
    abstract val elementTypeRef: CfirTypeRef
    abstract val sizeLiteral: String

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitVArrayTypeRef(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformVArrayTypeRef(this, data) as E
}
