

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.expressions

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.namedAccessExpression]
 */
abstract class CfirNamedAccessExpression : CfirQualifiedAccessExpression() {
    abstract override val source: CjSourceElement?
    abstract override val annotations: List<CfirAnnotation>
    abstract override val coneTypeOrNull: ConeCangJieType?
    abstract override val calleeReference: CfirReference
    abstract override val dispatchReceiver: CfirExpression?
    abstract override val typeArguments: List<CfirTypeRef>
    abstract override val explicitReceiver: CfirExpression?

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitNamedAccessExpression(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformNamedAccessExpression(this, data) as E

    abstract override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)

    abstract override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangJieType?)

    abstract override fun replaceCalleeReference(newCalleeReference: CfirReference)

    abstract override fun replaceDispatchReceiver(newDispatchReceiver: CfirExpression?)

    abstract override fun replaceTypeArguments(newTypeArguments: List<CfirTypeRef>)

    abstract override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirNamedAccessExpression

    abstract override fun <D> transformCalleeReference(transformer: CfirTransformer<D>, data: D): CfirNamedAccessExpression

    abstract override fun <D> transformTypeArguments(transformer: CfirTransformer<D>, data: D): CfirNamedAccessExpression

    abstract override fun <D> transformExplicitReceiver(transformer: CfirTransformer<D>, data: D): CfirNamedAccessExpression
}
