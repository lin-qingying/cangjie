

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.expressions.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.declarations.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirTypeOperationKind
import org.cangnova.cangjie.cfir.expressions.CfirTypeOperator
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

class CfirTypeOperatorImpl @CfirImplementationDetail constructor(
    override val source: CjSourceElement?,
    override var annotations: List<CfirAnnotation>,
    override var coneTypeOrNull: ConeCangjieType?,
    override val operation: CfirTypeOperationKind,
    override var argument: CfirExpression,
    override var typeRef: CfirTypeRef,
) : CfirTypeOperator() {

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        argument.accept(visitor, data)
        typeRef.accept(visitor, data)
    }

    override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)
     {
        this.annotations = newAnnotations
    }

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangjieType?)
     {
        this.coneTypeOrNull = newConeTypeOrNull
    }

    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirTypeOperator
     {
        this.annotations = annotations.map { it.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirAnnotation }
        return this
    }

    override fun <D> transformArgument(transformer: CfirTransformer<D>, data: D): CfirTypeOperator
     {
        this.argument = argument.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirExpression
        return this
    }

    override fun <D> transformTypeRef(transformer: CfirTransformer<D>, data: D): CfirTypeOperator
     {
        this.typeRef = typeRef.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirTypeRef
        return this
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirTypeOperatorImpl {
        transformAnnotations(transformer, data)
        transformArgument(transformer, data)
        transformTypeRef(transformer, data)
        return this
    }
}
