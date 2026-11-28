

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
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.patterns.CfirPatternTreeOwner
import org.cangnova.cangjie.cfir.symbols.CfirPatternBindingSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeSimpleCangJieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.cfir.visitors.transformInplace
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

@OptIn(CfirImplementationDetail::class, ResolveStateAccess::class)
class CfirPatternBindingVariableImpl @CfirImplementationDetail constructor(
    source: CjSourceElement?,
    override val moduleData: CfirModuleData,
    resolvePhase: CfirResolvePhase,
    annotations: MutableOrEmptyList<CfirAnnotation>,
    override val origin: CfirDeclarationOrigin,
    override val attributes: CfirDeclarationAttributes,
    override val isLocal: Boolean,
    deprecationsProvider: DeprecationsProvider,
    override var dispatchReceiverType: ConeSimpleCangJieType?,
    override var status: CfirDeclarationStatus,
    override val contentSource: CfirContentSource = CfirContentSource.Owned,
    override val isVar: Boolean,
    override val isTypeImplicit: Boolean,
    override val typeParameters: MutableList<CfirTypeParameter>,
    override val symbol: CfirPatternBindingSymbol,
    ownedInitializer: CfirExpression? = null,
    override var returnTypeRef: CfirTypeRef,
    override val name: Name,
) : CfirPatternBindingVariable() {
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
    override var initializer: CfirExpression?
        get() = ownedInitializer ?: org.cangnova.cangjie.cfir.patterns.wholeValueInitializer(patternTreeOwner, this)
        private set(value) {
            error("CfirPatternBindingVariable.initializer is derived from its owner and cannot be changed.")
        }
    override var ownedInitializer: CfirExpression? = ownedInitializer
        get() = when (val selectedSource = contentSource) {
            CfirContentSource.Owned -> field
            is CfirContentSource.Published -> selectedSource.snapshot().initializer(this)
        }
        set(value) {
            check(contentSource === CfirContentSource.Owned) {
                "Published Library content cannot be changed through an ordinary setter"
            }
            field = value
        }
    override var patternTreeOwner: CfirPatternTreeOwner? = null
        private set(value) {
            if (org.cangnova.cangjie.cfir.patterns.isPatternTreeRootRestoreInProgress()) {
                field = value
            } else {
                org.cangnova.cangjie.cfir.patterns.validatePatternTreeNodeOwnerChange(this, field, value)
                field = value
            }
        }

    init {
        if (contentSource is CfirContentSource.Published) {
            require(origin == CfirDeclarationOrigin.Library) { "Published content requires a Library declaration" }
            require(annotations.isEmpty()) { "Published content must be supplied by its completed snapshot" }
            require(deprecationsProvider === UnresolvedDeprecationProvider) { "Published content must be supplied by its completed snapshot" }
            require(ownedInitializer == null) { "Published content must be supplied by its completed snapshot" }
        }
        @Suppress("SENSELESS_COMPARISON")
        require(source != null || origin != CfirDeclarationOrigin.Source) { "${this::class.simpleName} with Source origin was instantiated without a source element." }
        symbol.bind(this)
        resolveState = resolvePhase.asResolveState()
    }

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        status.accept(visitor, data)
        typeParameters.forEach { it.accept(visitor, data) }
        ownedInitializer?.accept(visitor, data)
        returnTypeRef.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirPatternBindingVariableImpl {
        check(contentSource === CfirContentSource.Owned) { "Published Library children require an owned graph before transformation" }
        transformAnnotations(transformer, data)
        transformStatus(transformer, data)
        transformTypeParameters(transformer, data)
        transformOwnedInitializer(transformer, data)
        transformReturnTypeRef(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirPatternBindingVariableImpl {
        check(contentSource === CfirContentSource.Owned) { "Published Library children require an owned graph before transformation" }
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformStatus(transformer: CfirTransformer<D>, data: D): CfirPatternBindingVariableImpl {
        check(contentSource === CfirContentSource.Owned) { "Published Library children require an owned graph before transformation" }
        status = status.transform(transformer, data)
        return this
    }

    override fun <D> transformTypeParameters(transformer: CfirTransformer<D>, data: D): CfirPatternBindingVariableImpl {
        check(contentSource === CfirContentSource.Owned) { "Published Library children require an owned graph before transformation" }
        typeParameters.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformInitializer(transformer: CfirTransformer<D>, data: D): CfirPatternBindingVariableImpl {
        check(contentSource === CfirContentSource.Owned) { "Published Library children require an owned graph before transformation" }
        error("CfirPatternBindingVariable.initializer is derived from its owner and cannot be transformed independently.")
        return this
    }

    override fun <D> transformOwnedInitializer(transformer: CfirTransformer<D>, data: D): CfirPatternBindingVariableImpl {
        check(contentSource === CfirContentSource.Owned) { "Published Library children require an owned graph before transformation" }
        ownedInitializer = ownedInitializer?.transform(transformer, data)
        return this
    }

    override fun <D> transformReturnTypeRef(transformer: CfirTransformer<D>, data: D): CfirPatternBindingVariableImpl {
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

    override fun replaceDeprecationsProvider(newDeprecationsProvider: DeprecationsProvider) {
        deprecationsProvider = newDeprecationsProvider
    }

    override fun replaceDispatchReceiverType(newDispatchReceiverType: ConeSimpleCangJieType?) {
        dispatchReceiverType = newDispatchReceiverType
    }

    override fun replaceStatus(newStatus: CfirDeclarationStatus) {
        status = newStatus
    }

    override fun replaceInitializer(newInitializer: CfirExpression?) {
        initializer = newInitializer
    }

    override fun replaceOwnedInitializer(newOwnedInitializer: CfirExpression?) {
        ownedInitializer = newOwnedInitializer
    }

    override fun replacePatternTreeOwner(newPatternTreeOwner: CfirPatternTreeOwner?) {
        patternTreeOwner = newPatternTreeOwner
    }

    override fun replaceReturnTypeRef(newReturnTypeRef: CfirTypeRef) {
        returnTypeRef = newReturnTypeRef
    }
}
