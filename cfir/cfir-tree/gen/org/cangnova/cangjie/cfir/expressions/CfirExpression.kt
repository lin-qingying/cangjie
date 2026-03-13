

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.cangjie.cfir.expressions

import org.cangjie.cfir.CfirElement
import org.cangjie.cfir.CfirPureAbstractElement
import org.cangjie.cfir.common.CfirSourceElement
import org.cangjie.cfir.types.ConeCangjieType
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

/**
 * Generated from: [org.cangjie.cfir.tree.generator.CfirTree.expression]
 */
sealed class CfirExpression : CfirPureAbstractElement(), CfirElement, CfirStatement {
    abstract override val source: CfirSourceElement?
    abstract val coneTypeOrNull: ConeCangjieType?

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitExpression(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformExpression(this, data) as E
}
