package org.cangnova.cangjie.cfir.types.impl

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

/**
 * For special situations when resolution needs to happen during
 * [IMPLICIT_TYPES_BODY_RESOLVE][org.jetbrains.kotlin.fir.declarations.CfirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE]
 * but the type is already known. The primary use case is for REPL snippets,
 * where the `eval` function needs to be resolved during the implicit body phase,
 * but the return type is known to be [Unit].
 */
class ResolvedImplicitTypeRef(
    val typeRef: CfirResolvedTypeRef,
) : CfirImplicitTypeRef() {
    override val customRenderer: Boolean
        get() = false

    override val source: CjSourceElement? get() = null
    override val annotations: List<CfirAnnotation> get() = emptyList()

    override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>) {
    }

    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirImplicitTypeRef {
        return this
    }

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirElement {
        return this
    }
}
