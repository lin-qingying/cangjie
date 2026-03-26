

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirPureAbstractElement
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.annotation]
 */
abstract class CfirAnnotation : CfirPureAbstractElement(), CfirElement {
    abstract override val source: CjSourceElement?
    abstract val typeRef: CfirTypeRef
    abstract val arguments: List<CfirElement>

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitAnnotation(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformAnnotation(this, data) as E

    abstract fun <D> transformTypeRef(transformer: CfirTransformer<D>, data: D): CfirAnnotation


    abstract fun <D> transformArguments(transformer: CfirTransformer<D>, data: D): CfirAnnotation

}
