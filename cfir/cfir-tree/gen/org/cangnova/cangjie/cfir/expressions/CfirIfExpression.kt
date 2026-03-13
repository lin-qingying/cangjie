

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.cangnova.cangjie.cfir.expressions

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.ifExpression]
 */
abstract class CfirIfExpression : CfirExpression() {
    abstract override val source: CjSourceElement?
    abstract override var coneTypeOrNull: ConeCangjieType?
    abstract var condition: CfirExpression
    abstract var thenBranch: CfirBlock
    abstract var elseBranch: CfirExpression?

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitIfExpression(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformIfExpression(this, data) as E

    override abstract fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangjieType?)


    abstract fun <D> transformCondition(transformer: CfirTransformer<D>, data: D): CfirIfExpression


    abstract fun <D> transformThenBranch(transformer: CfirTransformer<D>, data: D): CfirIfExpression


    abstract fun <D> transformElseBranch(transformer: CfirTransformer<D>, data: D): CfirIfExpression

}
