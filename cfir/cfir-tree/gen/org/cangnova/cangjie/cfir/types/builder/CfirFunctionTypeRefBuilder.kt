

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.types.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.types.CfirFunctionTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.impl.CfirFunctionTypeRefImpl

@CfirBuilderDsl
class CfirFunctionTypeRefBuilder {
    val parameterTypeRefs: MutableList<CfirTypeRef> = mutableListOf()
    lateinit var returnTypeRef: CfirTypeRef

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirFunctionTypeRef {
        return CfirFunctionTypeRefImpl(
            parameterTypeRefs,
            returnTypeRef,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildFunctionTypeRef(init: CfirFunctionTypeRefBuilder.() -> Unit): CfirFunctionTypeRef {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirFunctionTypeRefBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildFunctionTypeRefCopy(original: CfirFunctionTypeRef, init: CfirFunctionTypeRefBuilder.() -> Unit): CfirFunctionTypeRef {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirFunctionTypeRefBuilder()
    copyBuilder.parameterTypeRefs.addAll(original.parameterTypeRefs)
    copyBuilder.returnTypeRef = original.returnTypeRef
    return copyBuilder.apply(init).build()
}
