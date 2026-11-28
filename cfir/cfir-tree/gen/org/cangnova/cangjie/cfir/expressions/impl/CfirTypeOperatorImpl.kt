

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.expressions.impl

import org.cangnova.cangjie.annotations.CangjieOverflowStrategy
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.MutableOrEmptyList
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirTypeOperationKind
import org.cangnova.cangjie.cfir.expressions.CfirTypeOperator
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.cfir.visitors.transformInplace
import org.cangnova.cangjie.source.CjSourceElement

class CfirTypeOperatorImpl @CfirImplementationDetail constructor(
    override var source: CjSourceElement?,
    override var annotations: MutableOrEmptyList<CfirAnnotation>,
    override var coneTypeOrNull: ConeCangJieType?,
    override var mappedExpression: CfirExpression?,
    override var overflowStrategy: CangjieOverflowStrategy?,
    override val operation: CfirTypeOperationKind,
    override var argument: CfirExpression,
    override var typeRef: CfirTypeRef,
) : CfirTypeOperator() {

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        argument.accept(visitor, data)
        typeRef.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirTypeOperatorImpl {
        transformAnnotations(transformer, data)
        transformArgument(transformer, data)
        transformTypeRef(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirTypeOperatorImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformArgument(transformer: CfirTransformer<D>, data: D): CfirTypeOperatorImpl {
        argument = argument.transform(transformer, data)
        return this
    }

    override fun <D> transformTypeRef(transformer: CfirTransformer<D>, data: D): CfirTypeOperatorImpl {
        typeRef = typeRef.transform(transformer, data)
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
}
