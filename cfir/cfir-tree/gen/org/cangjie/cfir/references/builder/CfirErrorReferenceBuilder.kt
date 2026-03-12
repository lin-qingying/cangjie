/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangjie.cfir.references.builder

import kotlin.contracts.*
import org.cangjie.cfir.CfirImplementationDetail
import org.cangjie.cfir.builder.CfirBuilderDsl
import org.cangjie.cfir.references.CfirErrorReference
import org.cangjie.cfir.references.impl.CfirErrorReferenceImpl

@CfirBuilderDsl
class CfirErrorReferenceBuilder {
    lateinit var reason: String

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirErrorReference {
        return CfirErrorReferenceImpl(
            reason,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildErrorReference(init: CfirErrorReferenceBuilder.() -> Unit): CfirErrorReference {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirErrorReferenceBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildErrorReferenceCopy(original: CfirErrorReference, init: CfirErrorReferenceBuilder.() -> Unit): CfirErrorReference {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirErrorReferenceBuilder()
    copyBuilder.reason = original.reason
    return copyBuilder.apply(init).build()
}
