

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
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.cfir.visitors.transformInplace
import org.cangnova.cangjie.source.CjSourceElement

class CfirQualifiedAccessExpressionImpl @CfirImplementationDetail constructor(
    override var source: CjSourceElement?,
    override var annotations: MutableOrEmptyList<CfirAnnotation>,
    override var coneTypeOrNull: ConeCangJieType?,
    override var mappedExpression: CfirExpression?,
    override var overflowStrategy: CangjieOverflowStrategy?,
    override var calleeReference: CfirReference,
    override var dispatchReceiver: CfirExpression?,
    override var explicitReceiver: CfirExpression?,
    override var typeArguments: MutableOrEmptyList<CfirTypeRef>,
) : CfirQualifiedAccessExpression() {

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        calleeReference.accept(visitor, data)
        explicitReceiver?.accept(visitor, data)
        if (dispatchReceiver !== explicitReceiver) {
            dispatchReceiver?.accept(visitor, data)
        }
        typeArguments.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirQualifiedAccessExpressionImpl {
        transformAnnotations(transformer, data)
        transformCalleeReference(transformer, data)
        val originalExplicitReceiver = explicitReceiver
        val originalDispatchReceiver = dispatchReceiver
        explicitReceiver = originalExplicitReceiver?.transform(transformer, data)
        dispatchReceiver = if (originalDispatchReceiver === originalExplicitReceiver) {
            explicitReceiver
        } else {
            originalDispatchReceiver?.transform(transformer, data)
        }
        transformTypeArguments(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirQualifiedAccessExpressionImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformCalleeReference(transformer: CfirTransformer<D>, data: D): CfirQualifiedAccessExpressionImpl {
        calleeReference = calleeReference.transform(transformer, data)
        return this
    }

    override fun <D> transformExplicitReceiver(transformer: CfirTransformer<D>, data: D): CfirQualifiedAccessExpressionImpl {
        explicitReceiver = explicitReceiver?.transform(transformer, data)
        return this
    }

    override fun <D> transformTypeArguments(transformer: CfirTransformer<D>, data: D): CfirQualifiedAccessExpressionImpl {
        typeArguments.transformInplace(transformer, data)
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

    override fun replaceCalleeReference(newCalleeReference: CfirReference) {
        calleeReference = newCalleeReference
    }

    override fun replaceDispatchReceiver(newDispatchReceiver: CfirExpression?) {
        dispatchReceiver = newDispatchReceiver
    }

    override fun replaceExplicitReceiver(newExplicitReceiver: CfirExpression?) {
        explicitReceiver = newExplicitReceiver
    }

    override fun replaceTypeArguments(newTypeArguments: List<CfirTypeRef>) {
        typeArguments = newTypeArguments.toMutableOrEmpty()
    }
}
