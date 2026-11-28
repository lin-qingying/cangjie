

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.expressions.impl

import org.cangnova.cangjie.annotations.CangjieOverflowStrategy
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

class CfirAnonymousFunctionExpressionImpl @CfirImplementationDetail constructor(
    override var source: CjSourceElement?,
    override var mappedExpression: CfirExpression?,
    override var overflowStrategy: CangjieOverflowStrategy?,
    override var anonymousFunction: CfirAnonymousFunction,
    override var isTrailingLambda: Boolean,
    override var supportMock: Boolean,
) : CfirAnonymousFunctionExpression() {
    override val annotations: List<CfirAnnotation>
        get() = anonymousFunction.annotations
    override val coneTypeOrNull: ConeCangJieType?
        get() = anonymousFunction.typeRef.coneTypeOrNull

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        anonymousFunction.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirAnonymousFunctionExpressionImpl {
        transformAnonymousFunction(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirAnonymousFunctionExpressionImpl {
        return this
    }

    override fun <D> transformAnonymousFunction(transformer: CfirTransformer<D>, data: D): CfirAnonymousFunctionExpressionImpl {
        anonymousFunction = anonymousFunction.transform(transformer, data)
        return this
    }

    @CfirImplementationDetail
    override fun replaceSource(newSource: CjSourceElement?) {
        source = newSource
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

    override fun replaceAnonymousFunction(newAnonymousFunction: CfirAnonymousFunction) {
        anonymousFunction = newAnonymousFunction
    }

    override fun replaceIsTrailingLambda(newIsTrailingLambda: Boolean) {
        isTrailingLambda = newIsTrailingLambda
    }

    override fun replaceSupportMock(newSupportMock: Boolean) {
        supportMock = newSupportMock
    }
}
