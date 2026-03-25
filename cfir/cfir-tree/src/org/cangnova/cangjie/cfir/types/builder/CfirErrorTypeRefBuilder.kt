package org.cangnova.cangjie.cfir.types.builder

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.declarations.CfirAnnotation
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.cfir.types.impl.CfirErrorTypeRefImpl
import org.cangnova.cangjie.source.CjSourceElement
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@CfirBuilderDsl
class CfirErrorTypeRefBuilder {
    var source: CjSourceElement? = null
    var annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneType: ConeCangJieType? = null
    var delegatedTypeRef: CfirTypeRef? = null
    var partiallyResolvedTypeRef: CfirTypeRef? = null
    lateinit var diagnostic: ConeDiagnostic

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirErrorTypeRef {
        return CfirErrorTypeRefImpl(
            source = source,
            annotations = annotations.toMutableList(),
            typeOrNull = coneType,
            delegatedTypeRef = delegatedTypeRef,
            diagnostic = diagnostic,
            partiallyResolvedTypeRef = partiallyResolvedTypeRef,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildErrorTypeRef(init: CfirErrorTypeRefBuilder.() -> Unit): CfirErrorTypeRef {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirErrorTypeRefBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildErrorTypeRefCopy(
    original: CfirErrorTypeRef,
    init: CfirErrorTypeRefBuilder.() -> Unit,
): CfirErrorTypeRef {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirErrorTypeRefBuilder().apply {
        source = original.source
        coneType = original.coneType
        annotations = original.annotations.toMutableList()
        delegatedTypeRef = original.delegatedTypeRef
        diagnostic = original.diagnostic
        partiallyResolvedTypeRef = original.partiallyResolvedTypeRef
    }.apply(init).build()
}
