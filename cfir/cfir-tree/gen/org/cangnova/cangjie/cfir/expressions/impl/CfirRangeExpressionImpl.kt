

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.expressions.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.MutableOrEmptyList
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirRangeExpression
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.cfir.visitors.transformInplace
import org.cangnova.cangjie.source.CjSourceElement

class CfirRangeExpressionImpl @CfirImplementationDetail constructor(
    override val source: CjSourceElement?,
    override var annotations: MutableOrEmptyList<CfirAnnotation>,
    override var coneTypeOrNull: ConeCangJieType?,
    override var start: CfirExpression,
    override var end: CfirExpression,
    override var step: CfirExpression?,
    override val isInclusive: Boolean,
) : CfirRangeExpression() {

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        start.accept(visitor, data)
        end.accept(visitor, data)
        step?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirRangeExpressionImpl {
        transformAnnotations(transformer, data)
        transformStart(transformer, data)
        transformEnd(transformer, data)
        transformStep(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirRangeExpressionImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformStart(transformer: CfirTransformer<D>, data: D): CfirRangeExpressionImpl {
        start = start.transform(transformer, data)
        return this
    }

    override fun <D> transformEnd(transformer: CfirTransformer<D>, data: D): CfirRangeExpressionImpl {
        end = end.transform(transformer, data)
        return this
    }

    override fun <D> transformStep(transformer: CfirTransformer<D>, data: D): CfirRangeExpressionImpl {
        step = step?.transform(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>) {
        annotations = newAnnotations.toMutableOrEmpty()
    }

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangJieType?) {
        coneTypeOrNull = newConeTypeOrNull
    }
}
