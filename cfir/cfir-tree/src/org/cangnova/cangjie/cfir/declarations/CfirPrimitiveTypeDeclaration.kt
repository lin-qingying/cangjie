package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.MutableOrEmptyList
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.symbols.CfirPrimitiveTypeSymbol
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.cfir.visitors.transformInplace
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

@OptIn(ResolveStateAccess::class, CfirImplementationDetail::class)
class CfirPrimitiveTypeDeclaration(
    override val moduleData: CfirModuleData,
    override val symbol: CfirPrimitiveTypeSymbol,
    override val name: Name,
    val kind: PrimitiveTypeKind,
    override var annotations: MutableOrEmptyList<CfirAnnotation> = MutableOrEmptyList.empty(),
    override val origin: CfirDeclarationOrigin = CfirDeclarationOrigin.Synthetic.Default,
    override val attributes: CfirDeclarationAttributes = CfirDeclarationAttributes.EMPTY,
    override var typeParameters: MutableList<CfirTypeParameterRef> = mutableListOf(),
    override var status: CfirDeclarationStatus = org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl(),
    override var declarations: MutableList<CfirDeclaration> = mutableListOf(),
    override var superTypeRefs: MutableList<CfirTypeRef> = mutableListOf(),
) : CfirClassLikeDeclaration() {
    override val source: CjSourceElement? = null

    init {
        symbol.bind(this)
        resolveState = CfirResolvePhase.RAW_CFIR.asResolveState()
    }

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        typeParameters.forEach { it.accept(visitor, data) }
        declarations.forEach { it.accept(visitor, data) }
        superTypeRefs.forEach { it.accept(visitor, data) }
    }

    override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>) {
        annotations = newAnnotations.toMutableOrEmpty()

    }

    override fun replaceStatus(newStatus: CfirDeclarationStatus) {
        status = newStatus
    }

    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirPrimitiveTypeDeclaration {
        annotations.transformInplace(transformer, data)

        return this
    }

    override fun <D> transformTypeParameters(transformer: CfirTransformer<D>, data: D): CfirPrimitiveTypeDeclaration {
        typeParameters = typeParameters.map { it.transform<CfirElement, D>(transformer, data) as CfirTypeParameterRef }.toMutableList()
        return this
    }

    override fun <D> transformStatus(transformer: CfirTransformer<D>, data: D): CfirPrimitiveTypeDeclaration {
        status = status.transform<CfirElement, D>(transformer, data) as CfirDeclarationStatus
        return this
    }

    override fun <D> transformDeclarations(transformer: CfirTransformer<D>, data: D): CfirPrimitiveTypeDeclaration {
        declarations = declarations.map { it.transform<CfirElement, D>(transformer, data) as CfirDeclaration }.toMutableList()
        return this
    }

    override fun <D> transformSuperTypeRefs(transformer: CfirTransformer<D>, data: D): CfirPrimitiveTypeDeclaration {
        superTypeRefs = superTypeRefs.map { it.transform<CfirElement, D>(transformer, data) as CfirTypeRef }.toMutableList()
        return this
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirPrimitiveTypeDeclaration {
        transformAnnotations(transformer, data)
        transformTypeParameters(transformer, data)
        transformStatus(transformer, data)
        transformDeclarations(transformer, data)
        transformSuperTypeRefs(transformer, data)
        return this
    }
}
