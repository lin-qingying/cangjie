

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
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.references.CfirControlFlowGraphReference
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeSimpleCangJieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.cfir.visitors.transformInplace
import org.cangnova.cangjie.source.CjSourceElement

@OptIn(CfirImplementationDetail::class, ResolveStateAccess::class)
class CfirConstructorImpl @CfirImplementationDetail constructor(
    override val source: CjSourceElement?,
    override val moduleData: CfirModuleData,
    resolvePhase: CfirResolvePhase,
    override var annotations: MutableOrEmptyList<CfirAnnotation>,
    override val origin: CfirDeclarationOrigin,
    override val attributes: CfirDeclarationAttributes,
    override val isLocal: Boolean,
    override val dispatchReceiverType: ConeSimpleCangJieType?,
    override var status: CfirDeclarationStatus,
    override val typeParameters: MutableList<CfirTypeParameter>,
    override var returnTypeRef: CfirTypeRef,
    override val valueParameters: MutableList<CfirValueParameter>,
    override var body: CfirBlock?,
    override val symbol: CfirConstructorSymbol,
) : CfirConstructor() {
    override var controlFlowGraphReference: CfirControlFlowGraphReference? = null
    override val isPrimary: Boolean
        get() = false

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
        valueParameters.forEach { it.accept(visitor, data) }
        body?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirConstructorImpl {
        transformAnnotations(transformer, data)
        controlFlowGraphReference = controlFlowGraphReference?.transform(transformer, data)
        transformTypeParameters(transformer, data)
        transformReturnTypeRef(transformer, data)
        transformValueParameters(transformer, data)
        transformBody(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirConstructorImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformStatus(transformer: CfirTransformer<D>, data: D): CfirConstructorImpl {
        return this
    }

    override fun <D> transformTypeParameters(transformer: CfirTransformer<D>, data: D): CfirConstructorImpl {
        typeParameters.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformReturnTypeRef(transformer: CfirTransformer<D>, data: D): CfirConstructorImpl {
        returnTypeRef = returnTypeRef.transform(transformer, data)
        return this
    }

    override fun <D> transformValueParameters(transformer: CfirTransformer<D>, data: D): CfirConstructorImpl {
        valueParameters.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformBody(transformer: CfirTransformer<D>, data: D): CfirConstructorImpl {
        body = body?.transform(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>) {
        annotations = newAnnotations.toMutableOrEmpty()
    }

    override fun replaceControlFlowGraphReference(newControlFlowGraphReference: CfirControlFlowGraphReference?) {
        controlFlowGraphReference = newControlFlowGraphReference
    }

    override fun replaceStatus(newStatus: CfirDeclarationStatus) {
        status = newStatus
    }

    override fun replaceReturnTypeRef(newReturnTypeRef: CfirTypeRef) {
        returnTypeRef = newReturnTypeRef
    }
}
