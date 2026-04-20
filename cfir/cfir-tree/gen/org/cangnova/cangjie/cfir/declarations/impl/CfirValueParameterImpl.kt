

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
import org.cangnova.cangjie.cfir.references.CfirControlFlowGraphReference
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeSimpleCangJieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.cfir.visitors.transformInplace
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

@OptIn(CfirImplementationDetail::class, ResolveStateAccess::class)
class CfirValueParameterImpl @CfirImplementationDetail constructor(
    override val source: CjSourceElement?,
    override val moduleData: CfirModuleData,
    resolvePhase: CfirResolvePhase,
    override var annotations: MutableOrEmptyList<CfirAnnotation>,
    override val origin: CfirDeclarationOrigin,
    override val attributes: CfirDeclarationAttributes,
    override val isLocal: Boolean,
    override var deprecationsProvider: DeprecationsProvider,
    override val dispatchReceiverType: ConeSimpleCangJieType?,
    override val symbol: CfirValueParameterSymbol,
    override val containingDeclarationSymbol: CfirBasedSymbol<*>,
    override val isNamed: Boolean,
    override var status: CfirDeclarationStatus,
    override val typeParameters: MutableList<CfirTypeParameter>,
    override var returnTypeRef: CfirTypeRef,
    override val name: Name,
    override var defaultValue: CfirExpression?,
) : CfirValueParameter() {
    override val initializer: CfirExpression?
        get() = null
    override val isVar: Boolean
        get() = false
    override var controlFlowGraphReference: CfirControlFlowGraphReference? = null

    init {
        symbol.bind(this)
        resolveState = resolvePhase.asResolveState()
        @Suppress("SENSELESS_COMPARISON")
        require(source != null || origin != CfirDeclarationOrigin.Source) { "${this::class.simpleName} with Source origin was instantiated without a source element." }
    }

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        controlFlowGraphReference?.accept(visitor, data)
        typeParameters.forEach { it.accept(visitor, data) }
        returnTypeRef.accept(visitor, data)
        defaultValue?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirValueParameterImpl {
        transformAnnotations(transformer, data)
        controlFlowGraphReference = controlFlowGraphReference?.transform(transformer, data)
        transformTypeParameters(transformer, data)
        transformReturnTypeRef(transformer, data)
        transformDefaultValue(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirValueParameterImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformInitializer(transformer: CfirTransformer<D>, data: D): CfirValueParameterImpl {
        return this
    }

    override fun <D> transformStatus(transformer: CfirTransformer<D>, data: D): CfirValueParameterImpl {
        return this
    }

    override fun <D> transformTypeParameters(transformer: CfirTransformer<D>, data: D): CfirValueParameterImpl {
        typeParameters.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformReturnTypeRef(transformer: CfirTransformer<D>, data: D): CfirValueParameterImpl {
        returnTypeRef = returnTypeRef.transform(transformer, data)
        return this
    }

    override fun <D> transformDefaultValue(transformer: CfirTransformer<D>, data: D): CfirValueParameterImpl {
        defaultValue = defaultValue?.transform(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>) {
        annotations = newAnnotations.toMutableOrEmpty()
    }

    override fun replaceDeprecationsProvider(newDeprecationsProvider: DeprecationsProvider) {
        deprecationsProvider = newDeprecationsProvider
    }

    override fun replaceInitializer(newInitializer: CfirExpression?) {}

    override fun replaceControlFlowGraphReference(newControlFlowGraphReference: CfirControlFlowGraphReference?) {
        controlFlowGraphReference = newControlFlowGraphReference
    }

    override fun replaceStatus(newStatus: CfirDeclarationStatus) {
        status = newStatus
    }

    override fun replaceReturnTypeRef(newReturnTypeRef: CfirTypeRef) {
        returnTypeRef = newReturnTypeRef
    }

    override fun replaceDefaultValue(newDefaultValue: CfirExpression?) {
        defaultValue = newDefaultValue
    }
}
