

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.cangnova.cangjie.cfir.expressions

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.subscriptExpression]
 */
abstract class CfirSubscriptExpression : CfirExpression() {
    abstract override val source: CjSourceElement?
    abstract override var coneTypeOrNull: ConeCangjieType?
    abstract var receiver: CfirExpression
    abstract var indices: List<CfirExpression>

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitSubscriptExpression(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformSubscriptExpression(this, data) as E

    override abstract fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangjieType?)


    abstract fun <D> transformReceiver(transformer: CfirTransformer<D>, data: D): CfirSubscriptExpression


    abstract fun <D> transformIndices(transformer: CfirTransformer<D>, data: D): CfirSubscriptExpression

}
