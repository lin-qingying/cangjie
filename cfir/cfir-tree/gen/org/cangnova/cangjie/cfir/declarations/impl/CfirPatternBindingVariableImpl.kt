

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
    override val source: CjSourceElement?,
    override val moduleData: CfirModuleData,
    resolvePhase: CfirResolvePhase,
    override var annotations: MutableOrEmptyList<CfirAnnotation>,
    override val origin: CfirDeclarationOrigin,
    override val attributes: CfirDeclarationAttributes,
    override val isLocal: Boolean,
    override var deprecationsProvider: DeprecationsProvider,
    override val dispatchReceiverType: ConeSimpleCangJieType?,
    override var status: CfirDeclarationStatus,
    override val isVar: Boolean,
    override val isTypeImplicit: Boolean,
    override val symbol: CfirPatternBindingSymbol,
    override val typeParameters: MutableList<CfirTypeParameter>,
    override var returnTypeRef: CfirTypeRef,
    override val name: Name,
) : CfirPatternBindingVariable() {
    override var initializer: CfirExpression?
        get() = org.cangnova.cangjie.cfir.patterns.wholeValueInitializer(patternTreeOwner, this)
        private set(value) {
            error("CfirPatternBindingVariable.initializer is derived from its owner and cannot be changed.")
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
        @Suppress("SENSELESS_COMPARISON")
        require(source != null || origin != CfirDeclarationOrigin.Source) { "${this::class.simpleName} with Source origin was instantiated without a source element." }
        symbol.bind(this)
        resolveState = resolvePhase.asResolveState()
    }

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        typeParameters.forEach { it.accept(visitor, data) }
        returnTypeRef.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirPatternBindingVariableImpl {
        transformAnnotations(transformer, data)
        transformTypeParameters(transformer, data)
        transformReturnTypeRef(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirPatternBindingVariableImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformStatus(transformer: CfirTransformer<D>, data: D): CfirPatternBindingVariableImpl {
        return this
    }

    override fun <D> transformTypeParameters(transformer: CfirTransformer<D>, data: D): CfirPatternBindingVariableImpl {
        typeParameters.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformInitializer(transformer: CfirTransformer<D>, data: D): CfirPatternBindingVariableImpl {
        error("CfirPatternBindingVariable.initializer is derived from its owner and cannot be transformed independently.")
        return this
    }

    override fun <D> transformReturnTypeRef(transformer: CfirTransformer<D>, data: D): CfirPatternBindingVariableImpl {
        returnTypeRef = returnTypeRef.transform(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>) {
        annotations = newAnnotations.toMutableOrEmpty()
    }

    override fun replaceDeprecationsProvider(newDeprecationsProvider: DeprecationsProvider) {
        deprecationsProvider = newDeprecationsProvider
    }

    override fun replaceStatus(newStatus: CfirDeclarationStatus) {
        status = newStatus
    }

    override fun replaceInitializer(newInitializer: CfirExpression?) {
        initializer = newInitializer
    }

    override fun replacePatternTreeOwner(newPatternTreeOwner: CfirPatternTreeOwner?) {
        patternTreeOwner = newPatternTreeOwner
    }

    override fun replaceReturnTypeRef(newReturnTypeRef: CfirTypeRef) {
        returnTypeRef = newReturnTypeRef
    }
}
