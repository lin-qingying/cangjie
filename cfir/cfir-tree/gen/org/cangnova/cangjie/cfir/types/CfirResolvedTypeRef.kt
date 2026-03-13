

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.resolvedTypeRef]
 */
abstract class CfirResolvedTypeRef : CfirTypeRef() {
    abstract override val source: CjSourceElement?
    abstract val coneType: ConeCangjieType

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitResolvedTypeRef(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformResolvedTypeRef(this, data) as E
}
