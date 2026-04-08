

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.expressions

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.tryExpression]
 */
abstract class CfirTryExpression : CfirExpression() {
    abstract override val source: CjSourceElement?
    abstract override val annotations: List<CfirAnnotation>
    abstract override val coneTypeOrNull: ConeCangJieType?
    abstract val tryBlock: CfirBlock
    abstract val handlers: List<CfirHandleClause>
    abstract val catches: List<CfirCatch>
    abstract val finallyBlock: CfirBlock?

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitTryExpression(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformTryExpression(this, data) as E

    abstract override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)

    abstract override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangJieType?)

    abstract override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirTryExpression

    abstract fun <D> transformTryBlock(transformer: CfirTransformer<D>, data: D): CfirTryExpression

    abstract fun <D> transformHandlers(transformer: CfirTransformer<D>, data: D): CfirTryExpression

    abstract fun <D> transformCatches(transformer: CfirTransformer<D>, data: D): CfirTryExpression

    abstract fun <D> transformFinallyBlock(transformer: CfirTransformer<D>, data: D): CfirTryExpression
}
