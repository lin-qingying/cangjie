

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.expressions.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.MutableOrEmptyList
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLetPatternExpression
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.cfir.visitors.transformInplace
import org.cangnova.cangjie.source.CjSourceElement

class CfirLetPatternExpressionImpl @CfirImplementationDetail constructor(
    override val source: CjSourceElement?,
    override var annotations: MutableOrEmptyList<CfirAnnotation>,
    override var coneTypeOrNull: ConeCangJieType?,
    override var initializer: CfirExpression,
    override var pattern: CfirPattern,
) : CfirLetPatternExpression() {

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        initializer.accept(visitor, data)
        pattern.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirLetPatternExpressionImpl {
        transformAnnotations(transformer, data)
        transformInitializer(transformer, data)
        transformPattern(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirLetPatternExpressionImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformInitializer(transformer: CfirTransformer<D>, data: D): CfirLetPatternExpressionImpl {
        initializer = initializer.transform(transformer, data)
        return this
    }

    override fun <D> transformPattern(transformer: CfirTransformer<D>, data: D): CfirLetPatternExpressionImpl {
        pattern = pattern.transform(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>) {
        annotations = newAnnotations.toMutableOrEmpty()
    }

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangJieType?) {
        coneTypeOrNull = newConeTypeOrNull
    }
}
