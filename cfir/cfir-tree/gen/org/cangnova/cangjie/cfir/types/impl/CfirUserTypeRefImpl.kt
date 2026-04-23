

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.types.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.CfirQualifierPart
import org.cangnova.cangjie.cfir.MutableOrEmptyList
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.types.CfirUserTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.cfir.visitors.transformInplace
import org.cangnova.cangjie.source.CjSourceElement

class CfirUserTypeRefImpl @CfirImplementationDetail constructor(
    override var annotations: MutableOrEmptyList<CfirAnnotation>,
    override val customRenderer: Boolean,
    override val source: CjSourceElement,
    override var qualifier: MutableOrEmptyList<CfirQualifierPart>,
) : CfirUserTypeRef() {

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        qualifier.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirUserTypeRefImpl {
        transformAnnotations(transformer, data)
        transformQualifier(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirUserTypeRefImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformQualifier(transformer: CfirTransformer<D>, data: D): CfirUserTypeRefImpl {
        qualifier.transformInplace(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>) {
        annotations = newAnnotations.toMutableOrEmpty()
    }

    override fun replaceQualifier(newQualifier: List<CfirQualifierPart>) {
        qualifier = newQualifier.toMutableOrEmpty()
    }
}
