

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.expressions.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.MutableOrEmptyList
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.cfir.visitors.transformInplace
import org.cangnova.cangjie.source.CjSourceElement

open class CfirFunctionCallImpl @CfirImplementationDetail constructor(
    override val source: CjSourceElement?,
    override var annotations: MutableOrEmptyList<CfirAnnotation>,
    override var coneTypeOrNull: ConeCangJieType?,
    override var calleeReference: CfirReference,
    override var dispatchReceiver: CfirExpression?,
    override var explicitReceiver: CfirExpression?,
    override var typeArguments: MutableOrEmptyList<CfirTypeRef>,
    override var argumentList: CfirArgumentList,
    override val origin: CfirFunctionCallOrigin,
    override val hasTrailingLambda: Boolean,
    override val varraySizeLiteral: String?,
) : CfirFunctionCall() {

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        calleeReference.accept(visitor, data)
        explicitReceiver?.accept(visitor, data)
        if (dispatchReceiver !== explicitReceiver) {
            dispatchReceiver?.accept(visitor, data)
        }
        typeArguments.forEach { it.accept(visitor, data) }
        argumentList.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirFunctionCallImpl {
        transformAnnotations(transformer, data)
        transformCalleeReference(transformer, data)
        explicitReceiver = explicitReceiver?.transform(transformer, data)
        if (dispatchReceiver !== explicitReceiver) {
            dispatchReceiver = dispatchReceiver?.transform(transformer, data)
        }
        transformTypeArguments(transformer, data)
        argumentList = argumentList.transform(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirFunctionCallImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformCalleeReference(transformer: CfirTransformer<D>, data: D): CfirFunctionCallImpl {
        calleeReference = calleeReference.transform(transformer, data)
        return this
    }

    override fun <D> transformExplicitReceiver(transformer: CfirTransformer<D>, data: D): CfirFunctionCallImpl {
        explicitReceiver = explicitReceiver?.transform(transformer, data)
        return this
    }

    override fun <D> transformTypeArguments(transformer: CfirTransformer<D>, data: D): CfirFunctionCallImpl {
        typeArguments.transformInplace(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>) {
        annotations = newAnnotations.toMutableOrEmpty()
    }

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangJieType?) {
        coneTypeOrNull = newConeTypeOrNull
    }

    override fun replaceCalleeReference(newCalleeReference: CfirReference) {
        calleeReference = newCalleeReference
    }

    override fun replaceDispatchReceiver(newDispatchReceiver: CfirExpression?) {
        dispatchReceiver = newDispatchReceiver
    }

    override fun replaceTypeArguments(newTypeArguments: List<CfirTypeRef>) {
        typeArguments = newTypeArguments.toMutableOrEmpty()
    }

    override fun replaceArgumentList(newArgumentList: CfirArgumentList) {
        argumentList = newArgumentList
    }
}
