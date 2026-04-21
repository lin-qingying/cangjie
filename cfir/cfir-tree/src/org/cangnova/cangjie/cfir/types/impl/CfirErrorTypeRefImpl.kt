package org.cangnova.cangjie.cfir.types.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.MutableOrEmptyList
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.cfir.visitors.transformInplace
import org.cangnova.cangjie.source.CjSourceElement

// Handwritten intentionally to preserve Kotlin FIR–style custom traversal semantics.
// delegatedTypeRef is intentionally skipped in acceptChildren/transformChildren to avoid duplicate visits.
class CfirErrorTypeRefImpl @CfirImplementationDetail constructor(
    override val source: CjSourceElement?,
    override var annotations: MutableOrEmptyList<CfirAnnotation>,
    typeOrNull: ConeCangJieType?,
    override var delegatedTypeRef: CfirTypeRef?,
    override val diagnostic: ConeDiagnostic,
    override var partiallyResolvedTypeRef: CfirTypeRef? = null,
) : CfirErrorTypeRef() {

    override val coneType: ConeCangJieType = typeOrNull ?: ConeErrorType(diagnostic)
    override val customRenderer: Boolean get() = false

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        partiallyResolvedTypeRef?.accept(visitor, data)
    }

    override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>) {
        annotations = newAnnotations.toMutableOrEmpty()
    }

    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirErrorTypeRef {
        annotations.transformInplace(transformer, data)

        return this
    }

    override fun <D> transformPartiallyResolvedTypeRef(transformer: CfirTransformer<D>, data: D): CfirErrorTypeRef {
        partiallyResolvedTypeRef = partiallyResolvedTypeRef?.transform(transformer, data)
        transformChildren(transformer, data)
        return this
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirErrorTypeRef {
        transformAnnotations(transformer, data)
        return this
    }
}
