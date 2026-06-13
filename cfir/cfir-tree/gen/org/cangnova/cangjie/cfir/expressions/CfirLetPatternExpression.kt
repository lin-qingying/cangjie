

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.expressions

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.letPatternExpression]
 */
abstract class CfirLetPatternExpression : CfirExpression() {
    abstract override val source: CjSourceElement?
    abstract override val annotations: List<CfirAnnotation>
    abstract override val coneTypeOrNull: ConeCangJieType?
    abstract val initializer: CfirExpression
    abstract val pattern: CfirPattern

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitLetPatternExpression(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformLetPatternExpression(this, data) as E

    abstract override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)

    abstract override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangJieType?)

    abstract override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirLetPatternExpression

    abstract fun <D> transformInitializer(transformer: CfirTransformer<D>, data: D): CfirLetPatternExpression

    abstract fun <D> transformPattern(transformer: CfirTransformer<D>, data: D): CfirLetPatternExpression
}
