

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.expressions.impl

import org.cangnova.cangjie.annotations.CangjieOverflowStrategy
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLazyBlock
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

class CfirLazyBlockImpl @CfirImplementationDetail constructor(
    override var mappedExpression: CfirExpression?,
    override var overflowStrategy: CangjieOverflowStrategy?,
) : CfirLazyBlock() {
    override val source: CjSourceElement?
        get() = error("CfirLazyBlock should be resolved before accessing")
    override val annotations: List<CfirAnnotation>
        get() = error("CfirLazyBlock should be resolved before accessing")
    override val coneTypeOrNull: ConeCangJieType?
        get() = error("CfirLazyBlock should be resolved before accessing")
    override val statements: List<CfirStatement>
        get() = error("CfirLazyBlock should be resolved before accessing")
    override val isUnsafe: Boolean
        get() = error("CfirLazyBlock should be resolved before accessing")

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirLazyBlockImpl {
        transformOtherChildren(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirLazyBlockImpl {
        return this
    }

    override fun <D> transformStatements(transformer: CfirTransformer<D>, data: D): CfirLazyBlockImpl {
        return this
    }

    override fun <D> transformOtherChildren(transformer: CfirTransformer<D>, data: D): CfirLazyBlockImpl {
        mappedExpression = mappedExpression?.transform(transformer, data)
        return this
    }

    @CfirImplementationDetail
    override fun replaceSource(newSource: CjSourceElement?) {
        require(newSource == source) { "An immutable source slot cannot accept another source." }
    }

    override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>) {}

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangJieType?) {
        require(newConeTypeOrNull == coneTypeOrNull) { "${javaClass.simpleName}.replaceConeTypeOrNull() called with invalid type '${newConeTypeOrNull}'. Current type is '$coneTypeOrNull'" }
    }

    override fun replaceMappedExpression(newMappedExpression: CfirExpression?) {
        mappedExpression = newMappedExpression
    }

    override fun replaceOverflowStrategy(newOverflowStrategy: CangjieOverflowStrategy?) {
        overflowStrategy = newOverflowStrategy
    }

    override fun replaceStatements(newStatements: List<CfirStatement>) {}

    override fun replaceIsUnsafe(newIsUnsafe: Boolean) {}
}
