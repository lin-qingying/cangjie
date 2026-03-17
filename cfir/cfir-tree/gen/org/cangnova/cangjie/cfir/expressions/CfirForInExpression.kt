

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.expressions

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirAnnotation
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.forInExpression]
 */
abstract class CfirForInExpression : CfirLoopExpression() {
    abstract override val source: CjSourceElement?
    abstract override val annotations: List<CfirAnnotation>
    abstract override val coneTypeOrNull: ConeCangjieType?
    abstract override val condition: CfirExpression
    abstract override val isDoWhile: Boolean
    abstract val variable: CfirVariable
    abstract val iterable: CfirExpression
    abstract override val body: CfirBlock

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitForInExpression(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformForInExpression(this, data) as E

    override abstract fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)


    override abstract fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangjieType?)


    override abstract fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirForInExpression


    override abstract fun <D> transformCondition(transformer: CfirTransformer<D>, data: D): CfirForInExpression


    abstract fun <D> transformVariable(transformer: CfirTransformer<D>, data: D): CfirForInExpression


    abstract fun <D> transformIterable(transformer: CfirTransformer<D>, data: D): CfirForInExpression


    override abstract fun <D> transformBody(transformer: CfirTransformer<D>, data: D): CfirForInExpression

}
