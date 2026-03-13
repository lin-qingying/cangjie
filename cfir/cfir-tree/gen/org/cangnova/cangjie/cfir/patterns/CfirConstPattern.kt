

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.cangnova.cangjie.cfir.patterns

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.constPattern]
 */
abstract class CfirConstPattern : CfirPattern() {
    abstract override val source: CjSourceElement?
    abstract var expression: CfirExpression

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitConstPattern(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformConstPattern(this, data) as E

    abstract fun <D> transformExpression(transformer: CfirTransformer<D>, data: D): CfirConstPattern

}
