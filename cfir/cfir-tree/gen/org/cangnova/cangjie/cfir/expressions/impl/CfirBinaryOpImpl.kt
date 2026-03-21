

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.expressions.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.declarations.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirBinaryOp
import org.cangnova.cangjie.cfir.expressions.CfirBinaryOpKind
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

class CfirBinaryOpImpl @CfirImplementationDetail constructor(
    override val source: CjSourceElement?,
    override var annotations: List<CfirAnnotation>,
    override var coneTypeOrNull: ConeCangjieType?,
    override val kind: CfirBinaryOpKind,
    override var left: CfirExpression,
    override var right: CfirExpression,
) : CfirBinaryOp() {

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        left.accept(visitor, data)
        right.accept(visitor, data)
    }

    override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)
     {
        this.annotations = newAnnotations
    }

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangjieType?)
     {
        this.coneTypeOrNull = newConeTypeOrNull
    }

    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirBinaryOp
     {
        this.annotations = annotations.map { it.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirAnnotation }
        return this
    }

    override fun <D> transformLeft(transformer: CfirTransformer<D>, data: D): CfirBinaryOp
     {
        this.left = left.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirExpression
        return this
    }

    override fun <D> transformRight(transformer: CfirTransformer<D>, data: D): CfirBinaryOp
     {
        this.right = right.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirExpression
        return this
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirBinaryOpImpl {
        transformAnnotations(transformer, data)
        transformLeft(transformer, data)
        transformRight(transformer, data)
        return this
    }
}
