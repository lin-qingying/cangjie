

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir

import org.cangnova.cangjie.cfir.declarations.CfirAnnotation
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.annotationContainer]
 */
interface CfirAnnotationContainer : CfirElement {
    override val source: CjSourceElement?
    val annotations: List<CfirAnnotation>

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitAnnotationContainer(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformAnnotationContainer(this, data) as E

    fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)


    fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirAnnotationContainer

}
