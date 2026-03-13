

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.types.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.types.CfirBasicTypeRef
import org.cangnova.cangjie.cfir.types.impl.CfirBasicTypeRefImpl
import org.cangnova.cangjie.name.Name

@CfirBuilderDsl
class CfirBasicTypeRefBuilder {
    lateinit var name: Name

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirBasicTypeRef {
        return CfirBasicTypeRefImpl(
            name,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildBasicTypeRef(init: CfirBasicTypeRefBuilder.() -> Unit): CfirBasicTypeRef {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirBasicTypeRefBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildBasicTypeRefCopy(original: CfirBasicTypeRef, init: CfirBasicTypeRefBuilder.() -> Unit): CfirBasicTypeRef {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirBasicTypeRefBuilder()
    copyBuilder.name = original.name
    return copyBuilder.apply(init).build()
}
