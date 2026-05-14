

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.expressions.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.MutableOrEmptyList
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.cfir.visitors.transformInplace
import org.cangnova.cangjie.source.CjSourceElement

class CfirTryExpressionImpl @CfirImplementationDetail constructor(
    override val source: CjSourceElement?,
    override var annotations: MutableOrEmptyList<CfirAnnotation>,
    override var coneTypeOrNull: ConeCangJieType?,
    override val resources: MutableList<CfirFieldVariable>,
    override var tryBlock: CfirBlock,
    override val handlers: MutableList<CfirHandleClause>,
    override val catches: MutableList<CfirCatch>,
    override var finallyBlock: CfirBlock?,
) : CfirTryExpression() {

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        resources.forEach { it.accept(visitor, data) }
        tryBlock.accept(visitor, data)
        handlers.forEach { it.accept(visitor, data) }
        catches.forEach { it.accept(visitor, data) }
        finallyBlock?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirTryExpressionImpl {
        transformAnnotations(transformer, data)
        transformResources(transformer, data)
        transformTryBlock(transformer, data)
        transformHandlers(transformer, data)
        transformCatches(transformer, data)
        transformFinallyBlock(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirTryExpressionImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformResources(transformer: CfirTransformer<D>, data: D): CfirTryExpressionImpl {
        resources.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformTryBlock(transformer: CfirTransformer<D>, data: D): CfirTryExpressionImpl {
        tryBlock = tryBlock.transform(transformer, data)
        return this
    }

    override fun <D> transformHandlers(transformer: CfirTransformer<D>, data: D): CfirTryExpressionImpl {
        handlers.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformCatches(transformer: CfirTransformer<D>, data: D): CfirTryExpressionImpl {
        catches.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformFinallyBlock(transformer: CfirTransformer<D>, data: D): CfirTryExpressionImpl {
        finallyBlock = finallyBlock?.transform(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>) {
        annotations = newAnnotations.toMutableOrEmpty()
    }

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangJieType?) {
        coneTypeOrNull = newConeTypeOrNull
    }
}
