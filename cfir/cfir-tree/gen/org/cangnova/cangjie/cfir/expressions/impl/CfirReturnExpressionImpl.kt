

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.expressions.impl

import org.cangnova.cangjie.annotations.CangjieOverflowStrategy
import org.cangnova.cangjie.cfir.*
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.cfir.visitors.transformInplace
import org.cangnova.cangjie.source.CjSourceElement

class CfirReturnExpressionImpl @CfirImplementationDetail constructor(
    override var source: CjSourceElement?,
    override var annotations: MutableOrEmptyList<CfirAnnotation>,
    override var coneTypeOrNull: ConeCangJieType?,
    override var mappedExpression: CfirExpression?,
    override var overflowStrategy: CangjieOverflowStrategy?,
    override val target: CfirTarget<CfirFunctionBodyOwner>,
    override var result: CfirExpression,
) : CfirReturnExpression() {

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        result.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirReturnExpressionImpl {
        transformResult(transformer, data)
        transformOtherChildren(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirReturnExpressionImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformResult(transformer: CfirTransformer<D>, data: D): CfirReturnExpressionImpl {
        result = result.transform(transformer, data)
        return this
    }

    override fun <D> transformOtherChildren(transformer: CfirTransformer<D>, data: D): CfirReturnExpressionImpl {
        transformAnnotations(transformer, data)
        mappedExpression = mappedExpression?.transform(transformer, data)
        return this
    }

    @CfirImplementationDetail
    override fun replaceSource(newSource: CjSourceElement?) {
        source = newSource
    }

    override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>) {
        annotations = newAnnotations.toMutableOrEmpty()
    }

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangJieType?) {
        coneTypeOrNull = newConeTypeOrNull
    }

    override fun replaceMappedExpression(newMappedExpression: CfirExpression?) {
        mappedExpression = newMappedExpression
    }

    override fun replaceOverflowStrategy(newOverflowStrategy: CangjieOverflowStrategy?) {
        overflowStrategy = newOverflowStrategy
    }

    override fun replaceResult(newResult: CfirExpression) {
        result = newResult
    }
}
