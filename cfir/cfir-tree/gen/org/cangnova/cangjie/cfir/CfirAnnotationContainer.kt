

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir

import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.annotationContainer]
 */
interface CfirAnnotationContainer : CfirElement {
    override val source: CjSourceElement?
    val annotations: List<CfirAnnotation>

    /** 访问本节点拥有的完整来源子图；独立于 ordinary visitor 的语法子树。 */
    @CfirImplementationDetail
    override fun <R, D> acceptSourceChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
    }

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitAnnotationContainer(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformAnnotationContainer(this, data) as E

    @CfirImplementationDetail
    override fun replaceSource(newSource: CjSourceElement?)

    fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)

    fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirAnnotationContainer
}
