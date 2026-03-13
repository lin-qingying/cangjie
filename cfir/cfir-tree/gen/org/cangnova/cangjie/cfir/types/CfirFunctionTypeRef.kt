

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.functionTypeRef]
 */
abstract class CfirFunctionTypeRef : CfirTypeRef() {
    abstract override val source: CjSourceElement?
    abstract var parameterTypeRefs: List<CfirTypeRef>
    abstract var returnTypeRef: CfirTypeRef

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitFunctionTypeRef(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformFunctionTypeRef(this, data) as E

    abstract fun <D> transformParameterTypeRefs(transformer: CfirTransformer<D>, data: D): CfirFunctionTypeRef


    abstract fun <D> transformReturnTypeRef(transformer: CfirTransformer<D>, data: D): CfirFunctionTypeRef

}
