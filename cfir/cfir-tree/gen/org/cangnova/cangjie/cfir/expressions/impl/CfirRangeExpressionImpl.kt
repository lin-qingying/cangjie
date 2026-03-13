

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.cangjie.cfir.expressions.impl

import org.cangjie.cfir.CfirImplementationDetail
import org.cangjie.cfir.common.CfirSourceElement
import org.cangjie.cfir.expressions.CfirExpression
import org.cangjie.cfir.expressions.CfirRangeExpression
import org.cangjie.cfir.types.ConeCangjieType
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

class CfirRangeExpressionImpl @CfirImplementationDetail constructor(
    override val coneTypeOrNull: ConeCangjieType?,
    override val start: CfirExpression,
    override val end: CfirExpression,
    override val isInclusive: Boolean,
) : CfirRangeExpression() {
    override val source: CfirSourceElement?
        get() = null

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        start.accept(visitor, data)
        end.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirRangeExpressionImpl {
        start.transform<org.cangjie.cfir.CfirElement, D>(transformer, data)
        end.transform<org.cangjie.cfir.CfirElement, D>(transformer, data)
        return this
    }
}
