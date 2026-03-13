

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.declarations.impl

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirAnnotation
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor

internal class CfirAnnotationImpl(
    override var typeRef: CfirTypeRef,
    override var arguments: List<CfirElement>,
) : CfirAnnotation() {
    override val source: CjSourceElement?
        get() = null

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
