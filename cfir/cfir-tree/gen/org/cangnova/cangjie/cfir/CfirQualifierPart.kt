

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir

import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRefOrigin
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
    abstract val typeUseOrigin: CfirTypeRefOrigin
    abstract val typeArguments: List<CfirTypeRef>

    /** 访问本节点拥有的完整来源子图；独立于 ordinary visitor 的语法子树。 */
    @CfirImplementationDetail
    override fun <R, D> acceptSourceChildren(visitor: CfirVisitor<R, D>, data: D) {
        typeArguments.forEach { it.accept(visitor, data) }
    }

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitQualifierPart(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformQualifierPart(this, data) as E

    @CfirImplementationDetail
    abstract override fun replaceSource(newSource: CjSourceElement?)

    abstract fun replaceTypeUseOrigin(newTypeUseOrigin: CfirTypeRefOrigin)

    abstract fun replaceTypeArguments(newTypeArguments: List<CfirTypeRef>)

    abstract fun <D> transformTypeArguments(transformer: CfirTransformer<D>, data: D): CfirQualifierPart
}
