

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
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.cfir.visitors.transformInplace
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

@OptIn(CfirImplementationDetail::class, ResolveStateAccess::class)
class CfirTypeParameterImpl @CfirImplementationDetail constructor(
    override val source: CjSourceElement?,
    override val moduleData: CfirModuleData,
    resolvePhase: CfirResolvePhase,
    override var annotations: MutableOrEmptyList<CfirAnnotation>,
    override val origin: CfirDeclarationOrigin,
    override val attributes: CfirDeclarationAttributes,
    override val containingDeclarationSymbol: CfirBasedSymbol<*>,
    override val symbol: CfirTypeParameterSymbol,
    override val name: Name,
    override val bounds: MutableList<CfirTypeRef>,
) : CfirTypeParameter() {

    init {
        symbol.bind(this)
        resolveState = resolvePhase.asResolveState()
        @Suppress("SENSELESS_COMPARISON")
        require(source != null || origin != CfirDeclarationOrigin.Source) { "${this::class.simpleName} with Source origin was instantiated without a source element." }
    }

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        bounds.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirTypeParameterImpl {
        transformAnnotations(transformer, data)
        transformBounds(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirTypeParameterImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformBounds(transformer: CfirTransformer<D>, data: D): CfirTypeParameterImpl {
        bounds.transformInplace(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>) {
        annotations = newAnnotations.toMutableOrEmpty()
    }
}
