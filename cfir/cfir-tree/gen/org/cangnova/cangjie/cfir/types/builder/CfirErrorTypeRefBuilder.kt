

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.types.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.impl.CfirErrorTypeRefImpl

@CfirBuilderDsl
class CfirErrorTypeRefBuilder {
    lateinit var reason: String

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirErrorTypeRef {
        return CfirErrorTypeRefImpl(
            reason,
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
inline fun buildErrorTypeRefCopy(original: CfirErrorTypeRef, init: CfirErrorTypeRefBuilder.() -> Unit): CfirErrorTypeRef {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirErrorTypeRefBuilder()
    copyBuilder.reason = original.reason
    return copyBuilder.apply(init).build()
}
