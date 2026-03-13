

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.declarations.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.name.Name

class CfirTypeAliasImpl @CfirImplementationDetail constructor(
    override val symbol: CfirSymbol<*>,
    override val origin: CfirDeclarationOrigin,
    override var annotations: List<CfirAnnotation>,
    override val moduleData: CfirModuleData,
    override var resolvePhase: CfirResolvePhase,
    override val attributes: CfirDeclarationAttributes,
    override var status: CfirDeclarationStatus,
    override var typeParameters: List<CfirTypeParameter>,
    override val name: Name,
    override var expandedTypeRef: CfirTypeRef,
) : CfirTypeAlias() {
    override val source: CjSourceElement?
        get() = null

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        typeParameters.forEach { it.accept(visitor, data) }
        expandedTypeRef.accept(visitor, data)
    }

    override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)
     {
        this.annotations = newAnnotations
    }

    override fun replaceResolvePhase(newResolvePhase: CfirResolvePhase)
     {
        this.resolvePhase = newResolvePhase
    }

    override fun replaceStatus(newStatus: CfirDeclarationStatus)
     {
        this.status = newStatus
    }

    override fun replaceExpandedTypeRef(newExpandedTypeRef: CfirTypeRef)
     {
        this.expandedTypeRef = newExpandedTypeRef
    }

    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirTypeAlias
     {
        this.annotations = annotations.map { it.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirAnnotation }
        return this
    }

    override fun <D> transformStatus(transformer: CfirTransformer<D>, data: D): CfirTypeAlias
     {
        this.status = status.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirDeclarationStatus
        return this
    }

    override fun <D> transformTypeParameters(transformer: CfirTransformer<D>, data: D): CfirTypeAlias
     {
        this.typeParameters = typeParameters.map { it.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirTypeParameter }
        return this
    }

    override fun <D> transformExpandedTypeRef(transformer: CfirTransformer<D>, data: D): CfirTypeAlias
     {
        this.expandedTypeRef = expandedTypeRef.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirTypeRef
        return this
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirTypeAliasImpl {
        transformAnnotations(transformer, data)
        transformTypeParameters(transformer, data)
        transformExpandedTypeRef(transformer, data)
        return this
    }
}
