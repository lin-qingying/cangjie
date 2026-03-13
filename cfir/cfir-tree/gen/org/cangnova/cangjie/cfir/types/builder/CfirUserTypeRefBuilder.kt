

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.types.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.CfirUserTypeRef
import org.cangnova.cangjie.cfir.types.impl.CfirUserTypeRefImpl
import org.cangnova.cangjie.name.Name

@CfirBuilderDsl
class CfirUserTypeRefBuilder {
    val qualifier: MutableList<Name> = mutableListOf()
    val typeArguments: MutableList<CfirTypeRef> = mutableListOf()

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirUserTypeRef {
        return CfirUserTypeRefImpl(
            qualifier,
            typeArguments,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildUserTypeRef(init: CfirUserTypeRefBuilder.() -> Unit = {}): CfirUserTypeRef {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirUserTypeRefBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildUserTypeRefCopy(original: CfirUserTypeRef, init: CfirUserTypeRefBuilder.() -> Unit = {}): CfirUserTypeRef {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirUserTypeRefBuilder()
    copyBuilder.qualifier.addAll(original.qualifier)
    copyBuilder.typeArguments.addAll(original.typeArguments)
    return copyBuilder.apply(init).build()
}
