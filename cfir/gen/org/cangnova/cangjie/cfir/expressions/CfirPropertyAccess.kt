

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.expressions

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirResolvable
import org.cangnova.cangjie.cfir.declarations.CfirAnnotation
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.propertyAccess]
 */
abstract class CfirPropertyAccess : CfirExpression(), CfirResolvable {
    abstract override val source: CjSourceElement?
    abstract override val annotations: List<CfirAnnotation>
    abstract override val coneTypeOrNull: ConeCangJieType?
    abstract override val calleeReference: CfirReference
    abstract val explicitReceiver: CfirExpression?

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitPropertyAccess(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformPropertyAccess(this, data) as E

    override abstract fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)


    override abstract fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangJieType?)


    override abstract fun replaceCalleeReference(newCalleeReference: CfirReference)


    override abstract fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirPropertyAccess


    override abstract fun <D> transformCalleeReference(transformer: CfirTransformer<D>, data: D): CfirPropertyAccess


    abstract fun <D> transformExplicitReceiver(transformer: CfirTransformer<D>, data: D): CfirPropertyAccess

}
