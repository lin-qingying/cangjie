

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.expressions

import org.cangnova.cangjie.annotations.CangjieOverflowStrategy
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.references.CfirSuperReference
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.superReceiverExpression]
 */
abstract class CfirSuperReceiverExpression : CfirQualifiedAccessExpression() {
    abstract override val source: CjSourceElement?
    abstract override val annotations: List<CfirAnnotation>
    abstract override val coneTypeOrNull: ConeCangJieType?
    abstract override val mappedExpression: CfirExpression?
    abstract override val overflowStrategy: CangjieOverflowStrategy?
    abstract override val dispatchReceiver: CfirExpression?
    abstract override val explicitReceiver: CfirExpression?
    abstract override val typeArguments: List<CfirTypeRef>
    abstract override val calleeReference: CfirSuperReference

    /** 访问本节点拥有的完整来源子图；独立于 ordinary visitor 的语法子树。 */
    @CfirImplementationDetail
    override fun <R, D> acceptSourceChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        dispatchReceiver?.accept(visitor, data)
        explicitReceiver?.accept(visitor, data)
        typeArguments.forEach { it.accept(visitor, data) }
        calleeReference.accept(visitor, data)
    }

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitSuperReceiverExpression(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformSuperReceiverExpression(this, data) as E

    @CfirImplementationDetail
    abstract override fun replaceSource(newSource: CjSourceElement?)

    abstract override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)

    abstract override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangJieType?)

    abstract override fun replaceMappedExpression(newMappedExpression: CfirExpression?)

    abstract override fun replaceOverflowStrategy(newOverflowStrategy: CangjieOverflowStrategy?)

    abstract override fun replaceDispatchReceiver(newDispatchReceiver: CfirExpression?)

    abstract override fun replaceExplicitReceiver(newExplicitReceiver: CfirExpression?)

    abstract override fun replaceTypeArguments(newTypeArguments: List<CfirTypeRef>)

    abstract fun replaceCalleeReference(newCalleeReference: CfirSuperReference)

    abstract override fun replaceCalleeReference(newCalleeReference: CfirReference)

    abstract override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirSuperReceiverExpression

    abstract override fun <D> transformExplicitReceiver(transformer: CfirTransformer<D>, data: D): CfirSuperReceiverExpression

    abstract override fun <D> transformTypeArguments(transformer: CfirTransformer<D>, data: D): CfirSuperReceiverExpression

    abstract override fun <D> transformCalleeReference(transformer: CfirTransformer<D>, data: D): CfirSuperReceiverExpression
}
