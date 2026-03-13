

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangjie.cfir.types.builder

import kotlin.contracts.*
import org.cangjie.cfir.CfirImplementationDetail
import org.cangjie.cfir.builder.CfirBuilderDsl
import org.cangjie.cfir.types.CfirTupleTypeRef
import org.cangjie.cfir.types.CfirTypeRef
import org.cangjie.cfir.types.impl.CfirTupleTypeRefImpl

@CfirBuilderDsl
class CfirTupleTypeRefBuilder {
    val elementTypeRefs: MutableList<CfirTypeRef> = mutableListOf()

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirTupleTypeRef {
        return CfirTupleTypeRefImpl(
            elementTypeRefs,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildTupleTypeRef(init: CfirTupleTypeRefBuilder.() -> Unit = {}): CfirTupleTypeRef {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirTupleTypeRefBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildTupleTypeRefCopy(original: CfirTupleTypeRef, init: CfirTupleTypeRefBuilder.() -> Unit = {}): CfirTupleTypeRef {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirTupleTypeRefBuilder()
    copyBuilder.elementTypeRefs.addAll(original.elementTypeRefs)
    return copyBuilder.apply(init).build()
}
