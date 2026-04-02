

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.expressions.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.MutableOrEmptyList
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirForInExpression
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.cfir.visitors.transformInplace
import org.cangnova.cangjie.source.CjSourceElement

class CfirForInExpressionImpl @CfirImplementationDetail constructor(
    override val source: CjSourceElement?,
    override var annotations: MutableOrEmptyList<CfirAnnotation>,
    override var coneTypeOrNull: ConeCangJieType?,
    override var condition: CfirExpression,
    override val isDoWhile: Boolean,
    override var variable: CfirPatternVariable,
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

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirForInExpressionImpl {
        transformAnnotations(transformer, data)
        transformCondition(transformer, data)
        transformVariable(transformer, data)
        transformIterable(transformer, data)
        transformBody(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirForInExpressionImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformCondition(transformer: CfirTransformer<D>, data: D): CfirForInExpressionImpl {
        condition = condition.transform(transformer, data)
        return this
    }

    override fun <D> transformVariable(transformer: CfirTransformer<D>, data: D): CfirForInExpressionImpl {
        variable = variable.transform(transformer, data)
        return this
    }

    override fun <D> transformIterable(transformer: CfirTransformer<D>, data: D): CfirForInExpressionImpl {
        iterable = iterable.transform(transformer, data)
        return this
    }

    override fun <D> transformBody(transformer: CfirTransformer<D>, data: D): CfirForInExpressionImpl {
        body = body.transform(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>) {
        annotations = newAnnotations.toMutableOrEmpty()
    }

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangJieType?) {
        coneTypeOrNull = newConeTypeOrNull
    }
}
