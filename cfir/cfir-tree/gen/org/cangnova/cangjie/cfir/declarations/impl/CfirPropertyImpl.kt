

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.declarations.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.references.CfirControlFlowGraphReference
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

@OptIn(CfirImplementationDetail::class)
class CfirPropertyImpl @CfirImplementationDetail constructor(
    override val source: CjSourceElement?,
    override val moduleData: CfirModuleData,
    override var annotations: List<CfirAnnotation>,
    override val origin: CfirDeclarationOrigin,
    override val attributes: CfirDeclarationAttributes,
    override val symbol: CfirPropertySymbol,
    override var status: CfirDeclarationStatus,
    override var typeParameters: List<CfirTypeParameter>,
    override var returnTypeRef: CfirTypeRef,
    override val name: Name,
    override var getter: CfirFunction?,
    override var setter: CfirFunction?,
) : CfirProperty() {
    override var controlFlowGraphReference: CfirControlFlowGraphReference? = null

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        controlFlowGraphReference?.accept(visitor, data)
        typeParameters.forEach { it.accept(visitor, data) }
        returnTypeRef.accept(visitor, data)
        getter?.accept(visitor, data)
        setter?.accept(visitor, data)
    }

    override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)
     {
        this.annotations = newAnnotations
    }

    override fun replaceControlFlowGraphReference(newControlFlowGraphReference: CfirControlFlowGraphReference?)
     {
        this.controlFlowGraphReference = newControlFlowGraphReference
    }

    override fun replaceStatus(newStatus: CfirDeclarationStatus)
     {
        this.status = newStatus
    }

    override fun replaceReturnTypeRef(newReturnTypeRef: CfirTypeRef)
     {
        this.returnTypeRef = newReturnTypeRef
    }

    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirProperty
     {
        this.annotations = annotations.map { it.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirAnnotation }
        return this
    }

    override fun <D> transformStatus(transformer: CfirTransformer<D>, data: D): CfirProperty
     {
        this.status = status.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirDeclarationStatus
        return this
    }

    override fun <D> transformTypeParameters(transformer: CfirTransformer<D>, data: D): CfirProperty
     {
        this.typeParameters = typeParameters.map { it.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirTypeParameter }
        return this
    }

    override fun <D> transformReturnTypeRef(transformer: CfirTransformer<D>, data: D): CfirProperty
     {
        this.returnTypeRef = returnTypeRef.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirTypeRef
        return this
    }

    override fun <D> transformGetter(transformer: CfirTransformer<D>, data: D): CfirProperty
     {
        this.getter = getter?.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirFunction?
        return this
    }

    override fun <D> transformSetter(transformer: CfirTransformer<D>, data: D): CfirProperty
     {
        this.setter = setter?.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirFunction?
        return this
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirPropertyImpl {
        transformAnnotations(transformer, data)
        controlFlowGraphReference?.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data)
        transformTypeParameters(transformer, data)
        transformReturnTypeRef(transformer, data)
        transformGetter(transformer, data)
        transformSetter(transformer, data)
        return this
    }
}
