

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir

import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.qualifierPart]
 */
abstract class CfirQualifierPart : CfirPureAbstractElement(), CfirElement {
    abstract override val source: CjSourceElement?
    abstract val name: Name
    abstract val typeArguments: List<CfirTypeRef>

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitQualifierPart(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformQualifierPart(this, data) as E

    abstract fun replaceTypeArguments(newTypeArguments: List<CfirTypeRef>)

    abstract fun <D> transformTypeArguments(transformer: CfirTransformer<D>, data: D): CfirQualifierPart
}
