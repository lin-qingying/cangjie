

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.expressions

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirAnnotation
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.functionCall]
 */
abstract class CfirFunctionCall : CfirExpression() {
    abstract override val source: CjSourceElement?
    abstract override val annotations: List<CfirAnnotation>
    abstract override val coneTypeOrNull: ConeCangjieType?
    abstract val calleeReference: CfirReference
    abstract val explicitReceiver: CfirExpression?
    abstract val arguments: List<CfirExpression>
    abstract val typeArguments: List<CfirTypeRef>

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitFunctionCall(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformFunctionCall(this, data) as E

    override abstract fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)


    override abstract fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangjieType?)


    abstract fun replaceTypeArguments(newTypeArguments: List<CfirTypeRef>)


    override abstract fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirFunctionCall


    abstract fun <D> transformCalleeReference(transformer: CfirTransformer<D>, data: D): CfirFunctionCall


    abstract fun <D> transformExplicitReceiver(transformer: CfirTransformer<D>, data: D): CfirFunctionCall


    abstract fun <D> transformArguments(transformer: CfirTransformer<D>, data: D): CfirFunctionCall


    abstract fun <D> transformTypeArguments(transformer: CfirTransformer<D>, data: D): CfirFunctionCall

}
