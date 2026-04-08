

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.declarations.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.MutableOrEmptyList
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.cfir.visitors.transformInplace
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

@OptIn(CfirImplementationDetail::class, ResolveStateAccess::class)
class CfirTypeAliasImpl @CfirImplementationDetail constructor(
    override val source: CjSourceElement?,
    override val moduleData: CfirModuleData,
    resolvePhase: CfirResolvePhase,
    override var annotations: MutableOrEmptyList<CfirAnnotation>,
    override val origin: CfirDeclarationOrigin,
    override val attributes: CfirDeclarationAttributes,
    override val declarations: MutableList<CfirDeclaration>,
    override val superTypeRefs: MutableList<CfirTypeRef>,
    override val symbol: CfirTypeAliasSymbol,
    override var status: CfirDeclarationStatus,
    override val typeParameters: MutableList<CfirTypeParameter>,
    override val name: Name,
    override var expandedTypeRef: CfirTypeRef,
) : CfirTypeAlias() {

    init {
        symbol.bind(this)
        resolveState = resolvePhase.asResolveState()
        @Suppress("SENSELESS_COMPARISON")
        require(source != null || origin != CfirDeclarationOrigin.Source) { "${this::class.simpleName} with Source origin was instantiated without a source element." }
    }

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        declarations.forEach { it.accept(visitor, data) }
        superTypeRefs.forEach { it.accept(visitor, data) }
        typeParameters.forEach { it.accept(visitor, data) }
        expandedTypeRef.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirTypeAliasImpl {
        transformAnnotations(transformer, data)
        transformDeclarations(transformer, data)
        transformSuperTypeRefs(transformer, data)
        transformTypeParameters(transformer, data)
        transformExpandedTypeRef(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirTypeAliasImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformDeclarations(transformer: CfirTransformer<D>, data: D): CfirTypeAliasImpl {
        declarations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformSuperTypeRefs(transformer: CfirTransformer<D>, data: D): CfirTypeAliasImpl {
        superTypeRefs.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformStatus(transformer: CfirTransformer<D>, data: D): CfirTypeAliasImpl {
        return this
    }

    override fun <D> transformTypeParameters(transformer: CfirTransformer<D>, data: D): CfirTypeAliasImpl {
        typeParameters.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformExpandedTypeRef(transformer: CfirTransformer<D>, data: D): CfirTypeAliasImpl {
        expandedTypeRef = expandedTypeRef.transform(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>) {
        annotations = newAnnotations.toMutableOrEmpty()
    }

    override fun replaceStatus(newStatus: CfirDeclarationStatus) {
        status = newStatus
    }

    override fun replaceExpandedTypeRef(newExpandedTypeRef: CfirTypeRef) {
        expandedTypeRef = newExpandedTypeRef
    }
}
