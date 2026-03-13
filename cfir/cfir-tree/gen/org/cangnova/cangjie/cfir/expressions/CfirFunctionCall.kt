

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.cangnova.cangjie.cfir.expressions

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.functionCall]
 */
abstract class CfirFunctionCall : CfirExpression() {
    abstract override val source: CjSourceElement?
    abstract override var coneTypeOrNull: ConeCangjieType?
    abstract var calleeReference: CfirReference
    abstract var explicitReceiver: CfirExpression?
    abstract var arguments: List<CfirExpression>
    abstract var typeArguments: List<CfirTypeRef>

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitFunctionCall(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformFunctionCall(this, data) as E

    override abstract fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangjieType?)


    abstract fun <D> transformCalleeReference(transformer: CfirTransformer<D>, data: D): CfirFunctionCall


    abstract fun <D> transformExplicitReceiver(transformer: CfirTransformer<D>, data: D): CfirFunctionCall


    abstract fun <D> transformArguments(transformer: CfirTransformer<D>, data: D): CfirFunctionCall


    abstract fun <D> transformTypeArguments(transformer: CfirTransformer<D>, data: D): CfirFunctionCall

}
