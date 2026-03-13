

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.name.Name

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.userTypeRef]
 */
abstract class CfirUserTypeRef : CfirTypeRef() {
    abstract override val source: CjSourceElement?
    abstract var qualifier: List<Name>
    abstract var typeArguments: List<CfirTypeRef>

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitUserTypeRef(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformUserTypeRef(this, data) as E

    abstract fun <D> transformTypeArguments(transformer: CfirTransformer<D>, data: D): CfirUserTypeRef

}
