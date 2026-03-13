

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.cangjie.cfir.expressions

import org.cangjie.cfir.CfirElement
import org.cangjie.cfir.common.CfirSourceElement
import org.cangjie.cfir.types.ConeCangjieType
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

/**
 * Generated from: [org.cangjie.cfir.tree.generator.CfirTree.jumpExpression]
 */
abstract class CfirJumpExpression : CfirExpression() {
    abstract override val source: CfirSourceElement?
    abstract override val coneTypeOrNull: ConeCangjieType?
    abstract val kind: CfirJumpKind

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitJumpExpression(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformJumpExpression(this, data) as E
}
