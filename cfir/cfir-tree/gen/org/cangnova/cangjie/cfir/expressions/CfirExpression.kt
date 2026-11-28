

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.expressions

import org.cangnova.cangjie.annotations.CangjieOverflowStrategy
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.CfirPureAbstractElement
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.expression]
 */
abstract class CfirExpression : CfirPureAbstractElement(), CfirStatement {
    abstract override val source: CjSourceElement?
    abstract override val annotations: List<CfirAnnotation>
    abstract val coneTypeOrNull: ConeCangJieType?
    abstract val mappedExpression: CfirExpression?
    abstract val overflowStrategy: CangjieOverflowStrategy?

    /** 访问本节点拥有的完整来源子图；独立于 ordinary visitor 的语法子树。 */
    @CfirImplementationDetail
    override fun <R, D> acceptSourceChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
    }

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitExpression(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformExpression(this, data) as E

    @CfirImplementationDetail
    abstract override fun replaceSource(newSource: CjSourceElement?)

    abstract override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)

    abstract fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangJieType?)

    abstract fun replaceMappedExpression(newMappedExpression: CfirExpression?)

    abstract fun replaceOverflowStrategy(newOverflowStrategy: CangjieOverflowStrategy?)

    abstract override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirExpression
}
