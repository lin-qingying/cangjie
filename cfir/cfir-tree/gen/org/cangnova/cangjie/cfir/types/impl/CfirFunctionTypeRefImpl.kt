

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.types.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.types.CfirFunctionTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor

class CfirFunctionTypeRefImpl @CfirImplementationDetail constructor(
    override var parameterTypeRefs: List<CfirTypeRef>,
    override var returnTypeRef: CfirTypeRef,
) : CfirFunctionTypeRef() {
    override val source: CjSourceElement?
        get() = null

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        parameterTypeRefs.forEach { it.accept(visitor, data) }
        returnTypeRef.accept(visitor, data)
    }

    override fun <D> transformParameterTypeRefs(transformer: CfirTransformer<D>, data: D): CfirFunctionTypeRef
     {
        this.parameterTypeRefs = parameterTypeRefs.map { it.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirTypeRef }
        return this
    }

    override fun <D> transformReturnTypeRef(transformer: CfirTransformer<D>, data: D): CfirFunctionTypeRef
     {
        this.returnTypeRef = returnTypeRef.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirTypeRef
        return this
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirFunctionTypeRefImpl {
        transformParameterTypeRefs(transformer, data)
        transformReturnTypeRef(transformer, data)
        return this
    }
}
