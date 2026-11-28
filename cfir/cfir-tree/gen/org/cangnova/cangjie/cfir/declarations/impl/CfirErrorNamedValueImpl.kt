

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.declarations.impl

import org.cangnova.cangjie.cfir.CfirContentSource
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.MutableOrEmptyList
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.symbols.CfirErrorNamedValueSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.cfir.types.ConeSimpleCangJieType
import org.cangnova.cangjie.cfir.types.impl.CfirErrorTypeRefImpl
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.cfir.visitors.transformInplace
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

@OptIn(CfirImplementationDetail::class, ResolveStateAccess::class)
internal class CfirErrorNamedValueImpl(
    source: CjSourceElement?,
    override val moduleData: CfirModuleData,
    resolvePhase: CfirResolvePhase,
    override val contentSource: CfirContentSource = CfirContentSource.Owned,
    annotations: MutableOrEmptyList<CfirAnnotation>,
    override val origin: CfirDeclarationOrigin,
    override val attributes: CfirDeclarationAttributes,
    override val typeParameters: MutableList<CfirTypeParameterRef>,
    override var status: CfirDeclarationStatus,
    deprecationsProvider: DeprecationsProvider,
    override val dispatchReceiverType: ConeSimpleCangJieType?,
    override val diagnostic: ConeDiagnostic,
    override val name: Name,
    override val symbol: CfirErrorNamedValueSymbol,
) : CfirErrorNamedValue() {
    override var source: CjSourceElement? = source
        set(value) {
            require(value != null || origin != CfirDeclarationOrigin.Source) {
                "A Source-origin declaration must retain its source element."
            }
            field = value
        }
    override var annotations: MutableOrEmptyList<CfirAnnotation> = annotations
        get() = when (val selectedSource = contentSource) {
            CfirContentSource.Owned -> field
            is CfirContentSource.Published -> selectedSource.snapshot().annotations(this).values
        }
        set(value) {
            check(contentSource === CfirContentSource.Owned) {
                "Published Library content cannot be changed through an ordinary setter"
            }
            field = value
        }
    override val isLocal: Boolean
        get() = false
    override var returnTypeRef: CfirTypeRef = CfirErrorTypeRefImpl(source, MutableOrEmptyList.empty(), null, null, diagnostic)
    override var deprecationsProvider: DeprecationsProvider = deprecationsProvider
        get() = when (val selectedSource = contentSource) {
            CfirContentSource.Owned -> field
            is CfirContentSource.Published -> selectedSource.snapshot().deprecations(this)
        }
        set(value) {
            check(contentSource === CfirContentSource.Owned) {
                "Published Library content cannot be changed through an ordinary setter"
            }
            field = value
        }

    init {
        if (contentSource is CfirContentSource.Published) {
            require(origin == CfirDeclarationOrigin.Library) { "Published content requires a Library declaration" }
            require(annotations.isEmpty()) { "Published content must be supplied by its completed snapshot" }
            require(deprecationsProvider === UnresolvedDeprecationProvider) { "Published content must be supplied by its completed snapshot" }
        }
        symbol.bind(this)
        resolveState = resolvePhase.asResolveState()
        @Suppress("SENSELESS_COMPARISON")
        require(source != null || origin != CfirDeclarationOrigin.Source) { "${this::class.simpleName} with Source origin was instantiated without a source element." }
    }

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        typeParameters.forEach { it.accept(visitor, data) }
        status.accept(visitor, data)
        returnTypeRef.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirErrorNamedValueImpl {
        check(contentSource === CfirContentSource.Owned) { "Published Library children require an owned graph before transformation" }
        transformAnnotations(transformer, data)
        transformTypeParameters(transformer, data)
        transformStatus(transformer, data)
        transformReturnTypeRef(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirErrorNamedValueImpl {
        check(contentSource === CfirContentSource.Owned) { "Published Library children require an owned graph before transformation" }
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformTypeParameters(transformer: CfirTransformer<D>, data: D): CfirErrorNamedValueImpl {
        check(contentSource === CfirContentSource.Owned) { "Published Library children require an owned graph before transformation" }
        typeParameters.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformStatus(transformer: CfirTransformer<D>, data: D): CfirErrorNamedValueImpl {
        check(contentSource === CfirContentSource.Owned) { "Published Library children require an owned graph before transformation" }
        status = status.transform(transformer, data)
        return this
    }

    override fun <D> transformReturnTypeRef(transformer: CfirTransformer<D>, data: D): CfirErrorNamedValueImpl {
        check(contentSource === CfirContentSource.Owned) { "Published Library children require an owned graph before transformation" }
        returnTypeRef = returnTypeRef.transform(transformer, data)
        return this
    }

    @CfirImplementationDetail
    override fun replaceSource(newSource: CjSourceElement?) {
        source = newSource
    }

    override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>) {
        annotations = newAnnotations.toMutableOrEmpty()
    }

    override fun replaceStatus(newStatus: CfirDeclarationStatus) {
        status = newStatus
    }

    override fun replaceReturnTypeRef(newReturnTypeRef: CfirTypeRef) {
        returnTypeRef = newReturnTypeRef
    }

    override fun replaceDeprecationsProvider(newDeprecationsProvider: DeprecationsProvider) {
        deprecationsProvider = newDeprecationsProvider
    }
}
