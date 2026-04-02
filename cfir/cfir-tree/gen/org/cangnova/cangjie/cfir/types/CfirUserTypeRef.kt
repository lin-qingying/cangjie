

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.userTypeRef]
 */
abstract class CfirUserTypeRef : CfirUnresolvedTypeRef() {
    abstract override val annotations: List<CfirAnnotation>
    abstract override val source: CjSourceElement
    abstract val qualifier: List<Name>
    abstract val typeArguments: List<CfirTypeRef>

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitUserTypeRef(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformUserTypeRef(this, data) as E

    abstract override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)

    abstract fun replaceTypeArguments(newTypeArguments: List<CfirTypeRef>)

    abstract override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirUserTypeRef

    abstract fun <D> transformTypeArguments(transformer: CfirTransformer<D>, data: D): CfirUserTypeRef
}
