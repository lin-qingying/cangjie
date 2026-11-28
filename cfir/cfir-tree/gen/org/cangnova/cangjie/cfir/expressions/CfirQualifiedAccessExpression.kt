

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.expressions

import org.cangnova.cangjie.annotations.CangjieOverflowStrategy
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.qualifiedAccessExpression]
 */
abstract class CfirQualifiedAccessExpression : CfirExpression(), CfirResolvable {
    abstract override val source: CjSourceElement?
    abstract override val annotations: List<CfirAnnotation>
    abstract override val coneTypeOrNull: ConeCangJieType?
    abstract override val mappedExpression: CfirExpression?
    abstract override val overflowStrategy: CangjieOverflowStrategy?
    abstract override val calleeReference: CfirReference
    abstract val dispatchReceiver: CfirExpression?
    abstract val explicitReceiver: CfirExpression?
    abstract val typeArguments: List<CfirTypeRef>

    /** 访问本节点拥有的完整来源子图；独立于 ordinary visitor 的语法子树。 */
    @CfirImplementationDetail
    override fun <R, D> acceptSourceChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        calleeReference.accept(visitor, data)
        dispatchReceiver?.accept(visitor, data)
        explicitReceiver?.accept(visitor, data)
        typeArguments.forEach { it.accept(visitor, data) }
    }

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitQualifiedAccessExpression(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformQualifiedAccessExpression(this, data) as E

    @CfirImplementationDetail
    abstract override fun replaceSource(newSource: CjSourceElement?)

    abstract override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)

    abstract override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangJieType?)

    abstract override fun replaceMappedExpression(newMappedExpression: CfirExpression?)

    abstract override fun replaceOverflowStrategy(newOverflowStrategy: CangjieOverflowStrategy?)

    abstract override fun replaceCalleeReference(newCalleeReference: CfirReference)

    abstract fun replaceDispatchReceiver(newDispatchReceiver: CfirExpression?)

    abstract fun replaceExplicitReceiver(newExplicitReceiver: CfirExpression?)

    abstract fun replaceTypeArguments(newTypeArguments: List<CfirTypeRef>)

    abstract override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirQualifiedAccessExpression

    abstract override fun <D> transformCalleeReference(transformer: CfirTransformer<D>, data: D): CfirQualifiedAccessExpression

    abstract fun <D> transformExplicitReceiver(transformer: CfirTransformer<D>, data: D): CfirQualifiedAccessExpression

    abstract fun <D> transformTypeArguments(transformer: CfirTransformer<D>, data: D): CfirQualifiedAccessExpression
}
