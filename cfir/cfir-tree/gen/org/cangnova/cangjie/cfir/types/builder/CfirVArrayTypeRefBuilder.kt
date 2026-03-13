

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangjie.cfir.types.builder

import kotlin.contracts.*
import org.cangjie.cfir.CfirImplementationDetail
import org.cangjie.cfir.builder.CfirBuilderDsl
import org.cangjie.cfir.types.CfirTypeRef
import org.cangjie.cfir.types.CfirVArrayTypeRef
import org.cangjie.cfir.types.impl.CfirVArrayTypeRefImpl

@CfirBuilderDsl
class CfirVArrayTypeRefBuilder {
    lateinit var elementTypeRef: CfirTypeRef
    lateinit var sizeLiteral: String

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirVArrayTypeRef {
        return CfirVArrayTypeRefImpl(
            elementTypeRef,
            sizeLiteral,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildVArrayTypeRef(init: CfirVArrayTypeRefBuilder.() -> Unit): CfirVArrayTypeRef {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirVArrayTypeRefBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildVArrayTypeRefCopy(original: CfirVArrayTypeRef, init: CfirVArrayTypeRefBuilder.() -> Unit): CfirVArrayTypeRef {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirVArrayTypeRefBuilder()
    copyBuilder.elementTypeRef = original.elementTypeRef
    copyBuilder.sizeLiteral = original.sizeLiteral
    return copyBuilder.apply(init).build()
}
