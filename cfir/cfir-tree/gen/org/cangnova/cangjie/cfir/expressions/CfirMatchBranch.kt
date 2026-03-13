

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.cangnova.cangjie.cfir.expressions

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.matchBranch]
 */
abstract class CfirMatchBranch : CfirExpression() {
    abstract override val source: CjSourceElement?
    abstract override var coneTypeOrNull: ConeCangjieType?
    abstract var pattern: CfirPattern
    abstract var guard: CfirExpression?
    abstract var body: CfirBlock

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitMatchBranch(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformMatchBranch(this, data) as E

    override abstract fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangjieType?)


    abstract fun <D> transformPattern(transformer: CfirTransformer<D>, data: D): CfirMatchBranch


    abstract fun <D> transformGuard(transformer: CfirTransformer<D>, data: D): CfirMatchBranch


    abstract fun <D> transformBody(transformer: CfirTransformer<D>, data: D): CfirMatchBranch

}
