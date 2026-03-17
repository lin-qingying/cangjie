

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.expressions.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.declarations.CfirAnnotation
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirForInExpression
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor

class CfirForInExpressionImpl @CfirImplementationDetail constructor(
    override val source: CjSourceElement?,
    override var annotations: List<CfirAnnotation>,
    override var coneTypeOrNull: ConeCangjieType?,
    override var condition: CfirExpression,
    override val isDoWhile: Boolean,
    override var variable: CfirVariable,
    override var iterable: CfirExpression,
    override var body: CfirBlock,
) : CfirForInExpression() {

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        condition.accept(visitor, data)
        variable.accept(visitor, data)
        iterable.accept(visitor, data)
        body.accept(visitor, data)
    }

    override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)
     {
        this.annotations = newAnnotations
    }

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangjieType?)
     {
        this.coneTypeOrNull = newConeTypeOrNull
    }

    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirForInExpression
     {
        this.annotations = annotations.map { it.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirAnnotation }
        return this
    }

    override fun <D> transformCondition(transformer: CfirTransformer<D>, data: D): CfirForInExpression
     {
        this.condition = condition.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirExpression
        return this
    }

    override fun <D> transformVariable(transformer: CfirTransformer<D>, data: D): CfirForInExpression
     {
        this.variable = variable.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirVariable
        return this
    }

    override fun <D> transformIterable(transformer: CfirTransformer<D>, data: D): CfirForInExpression
     {
        this.iterable = iterable.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirExpression
        return this
    }

    override fun <D> transformBody(transformer: CfirTransformer<D>, data: D): CfirForInExpression
     {
        this.body = body.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirBlock
        return this
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirForInExpressionImpl {
        transformAnnotations(transformer, data)
        transformCondition(transformer, data)
        transformVariable(transformer, data)
        transformIterable(transformer, data)
        transformBody(transformer, data)
        return this
    }
}
