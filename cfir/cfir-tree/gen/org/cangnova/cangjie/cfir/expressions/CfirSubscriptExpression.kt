

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.expressions

import org.cangnova.cangjie.annotations.CangjieOverflowStrategy
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.subscriptExpression]
 */
abstract class CfirSubscriptExpression : CfirExpression() {
    abstract override val source: CjSourceElement?
    abstract override val annotations: List<CfirAnnotation>
    abstract override val coneTypeOrNull: ConeCangJieType?
    abstract override val mappedExpression: CfirExpression?
    abstract override val overflowStrategy: CangjieOverflowStrategy?
    abstract val receiver: CfirExpression
    abstract val indices: List<CfirExpression>
    abstract val isTupleAccess: Boolean
    abstract val resolvedGetCall: CfirFunctionCall?
    abstract val resolvedSetCall: CfirFunctionCall?

    /** 访问本节点拥有的完整来源子图；独立于 ordinary visitor 的语法子树。 */
    @CfirImplementationDetail
    override fun <R, D> acceptSourceChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        receiver.accept(visitor, data)
        indices.forEach { it.accept(visitor, data) }
        resolvedGetCall?.accept(visitor, data)
        resolvedSetCall?.accept(visitor, data)
    }

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitSubscriptExpression(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformSubscriptExpression(this, data) as E

    @CfirImplementationDetail
    abstract override fun replaceSource(newSource: CjSourceElement?)

    abstract override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)

    abstract override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangJieType?)

    abstract override fun replaceMappedExpression(newMappedExpression: CfirExpression?)

    abstract override fun replaceOverflowStrategy(newOverflowStrategy: CangjieOverflowStrategy?)

    abstract fun replaceIsTupleAccess(newIsTupleAccess: Boolean)

    abstract fun replaceResolvedGetCall(newResolvedGetCall: CfirFunctionCall?)

    abstract fun replaceResolvedSetCall(newResolvedSetCall: CfirFunctionCall?)

    abstract override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirSubscriptExpression

    abstract fun <D> transformReceiver(transformer: CfirTransformer<D>, data: D): CfirSubscriptExpression

    abstract fun <D> transformIndices(transformer: CfirTransformer<D>, data: D): CfirSubscriptExpression
}
