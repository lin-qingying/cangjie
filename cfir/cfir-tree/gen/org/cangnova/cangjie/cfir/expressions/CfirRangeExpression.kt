

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.expressions

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirAnnotation
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.rangeExpression]
 */
abstract class CfirRangeExpression : CfirExpression() {
    abstract override val source: CjSourceElement?
    abstract override val annotations: List<CfirAnnotation>
    abstract override val coneTypeOrNull: ConeCangjieType?
    abstract val start: CfirExpression
    abstract val end: CfirExpression
    abstract val isInclusive: Boolean

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitRangeExpression(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformRangeExpression(this, data) as E

    override abstract fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)


    override abstract fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangjieType?)


    override abstract fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirRangeExpression


    abstract fun <D> transformStart(transformer: CfirTransformer<D>, data: D): CfirRangeExpression


    abstract fun <D> transformEnd(transformer: CfirTransformer<D>, data: D): CfirRangeExpression

}
