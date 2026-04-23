

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirQualifierPart
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.userTypeRef]
 */
abstract class CfirUserTypeRef : CfirUnresolvedTypeRef() {
    abstract override val annotations: List<CfirAnnotation>
    abstract override val customRenderer: Boolean
    abstract override val source: CjSourceElement
    abstract val qualifier: List<CfirQualifierPart>

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitUserTypeRef(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformUserTypeRef(this, data) as E

    abstract override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)

    abstract fun replaceQualifier(newQualifier: List<CfirQualifierPart>)

    abstract override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirUserTypeRef

    abstract fun <D> transformQualifier(transformer: CfirTransformer<D>, data: D): CfirUserTypeRef
}
