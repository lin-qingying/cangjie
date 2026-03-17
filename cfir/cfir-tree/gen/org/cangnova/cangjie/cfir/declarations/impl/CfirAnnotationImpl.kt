

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.declarations.impl

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirAnnotation
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor

internal class CfirAnnotationImpl(
    override val source: CjSourceElement?,
    override var typeRef: CfirTypeRef,
    override var arguments: List<CfirElement>,
) : CfirAnnotation() {

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        typeRef.accept(visitor, data)
        arguments.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformTypeRef(transformer: CfirTransformer<D>, data: D): CfirAnnotation
     {
        this.typeRef = typeRef.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirTypeRef
        return this
    }

    override fun <D> transformArguments(transformer: CfirTransformer<D>, data: D): CfirAnnotation
     {
        this.arguments = arguments.map { it.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirElement }
        return this
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirAnnotationImpl {
        transformTypeRef(transformer, data)
        transformArguments(transformer, data)
        return this
    }
}
