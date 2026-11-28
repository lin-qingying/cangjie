

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.expressions

import org.cangnova.cangjie.annotations.CangjieOverflowStrategy
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirFunctionBodyOwner
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.CfirTarget
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.returnExpression]
 */
abstract class CfirReturnExpression : CfirJump<CfirFunctionBodyOwner>() {
    abstract override val source: CjSourceElement?
    abstract override val annotations: List<CfirAnnotation>
    abstract override val coneTypeOrNull: ConeCangJieType?
    abstract override val mappedExpression: CfirExpression?
    abstract override val overflowStrategy: CangjieOverflowStrategy?
    abstract override val target: CfirTarget<CfirFunctionBodyOwner>
    abstract val result: CfirExpression

    /** 访问本节点拥有的完整来源子图；独立于 ordinary visitor 的语法子树。 */
    @CfirImplementationDetail
    override fun <R, D> acceptSourceChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        result.accept(visitor, data)
    }

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitReturnExpression(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformReturnExpression(this, data) as E

    @CfirImplementationDetail
    abstract override fun replaceSource(newSource: CjSourceElement?)

    abstract override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)

    abstract override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangJieType?)

    abstract override fun replaceMappedExpression(newMappedExpression: CfirExpression?)

    abstract override fun replaceOverflowStrategy(newOverflowStrategy: CangjieOverflowStrategy?)

    abstract fun replaceResult(newResult: CfirExpression)

    abstract override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirReturnExpression

    abstract fun <D> transformResult(transformer: CfirTransformer<D>, data: D): CfirReturnExpression

    abstract fun <D> transformOtherChildren(transformer: CfirTransformer<D>, data: D): CfirReturnExpression
}
