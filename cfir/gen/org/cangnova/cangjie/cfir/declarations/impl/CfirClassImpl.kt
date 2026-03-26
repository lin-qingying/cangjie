

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.declarations.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.references.CfirControlFlowGraphReference
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

@OptIn(CfirImplementationDetail::class)
class CfirClassImpl @CfirImplementationDetail constructor(
    override val source: CjSourceElement?,
    override val moduleData: CfirModuleData,
    override var annotations: List<CfirAnnotation>,
    override val origin: CfirDeclarationOrigin,
    override val attributes: CfirDeclarationAttributes,
    override var status: CfirDeclarationStatus,
    override var typeParameters: List<CfirTypeParameter>,
    override val symbol: CfirClassSymbol,
    override var superTypeRefs: List<CfirTypeRef>,
    override var declarations: List<CfirDeclaration>,
    override val name: Name,
) : CfirClass() {
    override var controlFlowGraphReference: CfirControlFlowGraphReference? = null

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        controlFlowGraphReference?.accept(visitor, data)
        typeParameters.forEach { it.accept(visitor, data) }
        superTypeRefs.forEach { it.accept(visitor, data) }
        declarations.forEach { it.accept(visitor, data) }
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

    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirClass
     {
        this.annotations = annotations.map { it.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirAnnotation }
        return this
    }

    override fun <D> transformStatus(transformer: CfirTransformer<D>, data: D): CfirClass
     {
        this.status = status.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirDeclarationStatus
        return this
    }

    override fun <D> transformTypeParameters(transformer: CfirTransformer<D>, data: D): CfirClass
     {
        this.typeParameters = typeParameters.map { it.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirTypeParameter }
        return this
    }

    override fun <D> transformSuperTypeRefs(transformer: CfirTransformer<D>, data: D): CfirClass
     {
        this.superTypeRefs = superTypeRefs.map { it.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirTypeRef }
        return this
    }

    override fun <D> transformDeclarations(transformer: CfirTransformer<D>, data: D): CfirClass
     {
        this.declarations = declarations.map { it.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirDeclaration }
        return this
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirClassImpl {
        transformAnnotations(transformer, data)
        controlFlowGraphReference?.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data)
        transformTypeParameters(transformer, data)
        transformSuperTypeRefs(transformer, data)
        transformDeclarations(transformer, data)
        return this
    }
}
