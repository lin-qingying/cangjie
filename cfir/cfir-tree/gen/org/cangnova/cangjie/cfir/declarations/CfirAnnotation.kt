

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirPureAbstractElement
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.annotation]
 */
abstract class CfirAnnotation : CfirPureAbstractElement(), CfirElement {
    abstract override val source: CjSourceElement?
    abstract var typeRef: CfirTypeRef
    abstract var arguments: List<CfirElement>

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitAnnotation(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformAnnotation(this, data) as E

    abstract fun <D> transformTypeRef(transformer: CfirTransformer<D>, data: D): CfirAnnotation


    abstract fun <D> transformArguments(transformer: CfirTransformer<D>, data: D): CfirAnnotation

}
