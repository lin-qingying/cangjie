

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.declarations.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.name.Name

class CfirValueParameterImpl @CfirImplementationDetail constructor(
    override val symbol: CfirSymbol<*>,
    override val origin: CfirDeclarationOrigin,
    override var annotations: List<CfirAnnotation>,
    override val moduleData: CfirModuleData,
    override var resolvePhase: CfirResolvePhase,
    override val attributes: CfirDeclarationAttributes,
    override var status: CfirDeclarationStatus,
    override var typeParameters: List<CfirTypeParameter>,
    override var returnTypeRef: CfirTypeRef,
    override val name: Name,
    override var defaultValue: CfirExpression?,
) : CfirValueParameter() {
    override val source: CjSourceElement?
        get() = null

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        typeParameters.forEach { it.accept(visitor, data) }
        returnTypeRef.accept(visitor, data)
        defaultValue?.accept(visitor, data)
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

    override fun replaceReturnTypeRef(newReturnTypeRef: CfirTypeRef)
     {
        this.returnTypeRef = newReturnTypeRef
    }

    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirValueParameter
     {
        this.annotations = annotations.map { it.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirAnnotation }
        return this
    }

    override fun <D> transformStatus(transformer: CfirTransformer<D>, data: D): CfirValueParameter
     {
        this.status = status.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirDeclarationStatus
        return this
    }

    override fun <D> transformTypeParameters(transformer: CfirTransformer<D>, data: D): CfirValueParameter
     {
        this.typeParameters = typeParameters.map { it.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirTypeParameter }
        return this
    }

    override fun <D> transformReturnTypeRef(transformer: CfirTransformer<D>, data: D): CfirValueParameter
     {
        this.returnTypeRef = returnTypeRef.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirTypeRef
        return this
    }

    override fun <D> transformDefaultValue(transformer: CfirTransformer<D>, data: D): CfirValueParameter
     {
        this.defaultValue = defaultValue?.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirExpression?
        return this
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirValueParameterImpl {
        transformAnnotations(transformer, data)
        transformTypeParameters(transformer, data)
        transformReturnTypeRef(transformer, data)
        transformDefaultValue(transformer, data)
        return this
    }
}
