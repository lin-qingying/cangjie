

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.cangnova.cangjie.cfir.expressions

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.forInExpression]
 */
abstract class CfirForInExpression : CfirLoopExpression() {
    abstract override val source: CjSourceElement?
    abstract override var coneTypeOrNull: ConeCangjieType?
    abstract override var condition: CfirExpression
    abstract override val isDoWhile: Boolean
    abstract var variable: CfirVariable
    abstract var iterable: CfirExpression
    abstract override var body: CfirBlock

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitForInExpression(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformForInExpression(this, data) as E

    override abstract fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangjieType?)


    override abstract fun <D> transformCondition(transformer: CfirTransformer<D>, data: D): CfirForInExpression


    abstract fun <D> transformVariable(transformer: CfirTransformer<D>, data: D): CfirForInExpression


    abstract fun <D> transformIterable(transformer: CfirTransformer<D>, data: D): CfirForInExpression


    override abstract fun <D> transformBody(transformer: CfirTransformer<D>, data: D): CfirForInExpression

}
