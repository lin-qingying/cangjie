

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.expressions.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.declarations.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirCatch
import org.cangnova.cangjie.cfir.expressions.CfirTryExpression
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor

class CfirTryExpressionImpl @CfirImplementationDetail constructor(
    override val source: CjSourceElement?,
    override var annotations: List<CfirAnnotation>,
    override var coneTypeOrNull: ConeCangjieType?,
    override var tryBlock: CfirBlock,
    override var catches: List<CfirCatch>,
    override var finallyBlock: CfirBlock?,
) : CfirTryExpression() {

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        tryBlock.accept(visitor, data)
        catches.forEach { it.accept(visitor, data) }
        finallyBlock?.accept(visitor, data)
    }

    override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)
     {
        this.annotations = newAnnotations
    }

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangjieType?)
     {
        this.coneTypeOrNull = newConeTypeOrNull
    }

    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirTryExpression
     {
        this.annotations = annotations.map { it.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirAnnotation }
        return this
    }

    override fun <D> transformTryBlock(transformer: CfirTransformer<D>, data: D): CfirTryExpression
     {
        this.tryBlock = tryBlock.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirBlock
        return this
    }

    override fun <D> transformCatches(transformer: CfirTransformer<D>, data: D): CfirTryExpression
     {
        this.catches = catches.map { it.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirCatch }
        return this
    }

    override fun <D> transformFinallyBlock(transformer: CfirTransformer<D>, data: D): CfirTryExpression
     {
        this.finallyBlock = finallyBlock?.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirBlock?
        return this
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirTryExpressionImpl {
        transformAnnotations(transformer, data)
        transformTryBlock(transformer, data)
        transformCatches(transformer, data)
        transformFinallyBlock(transformer, data)
        return this
    }
}
