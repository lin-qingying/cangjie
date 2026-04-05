

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.expressions

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.call]
 */
sealed interface CfirCall : CfirStatement {
    override val source: CjSourceElement?
    override val annotations: List<CfirAnnotation>
    val argumentList: CfirArgumentList

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitCall(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformCall(this, data) as E

    override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)

    fun replaceArgumentList(newArgumentList: CfirArgumentList)

    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirCall
}
