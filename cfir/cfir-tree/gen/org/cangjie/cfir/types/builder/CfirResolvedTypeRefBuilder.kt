/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangjie.cfir.types.builder

import kotlin.contracts.*
import org.cangjie.cfir.CfirImplementationDetail
import org.cangjie.cfir.builder.CfirBuilderDsl
import org.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangjie.cfir.types.ConeCangjieType
import org.cangjie.cfir.types.impl.CfirResolvedTypeRefImpl

@CfirBuilderDsl
class CfirResolvedTypeRefBuilder {
    lateinit var coneType: ConeCangjieType

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirResolvedTypeRef {
        return CfirResolvedTypeRefImpl(
            coneType,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildResolvedTypeRef(init: CfirResolvedTypeRefBuilder.() -> Unit): CfirResolvedTypeRef {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirResolvedTypeRefBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildResolvedTypeRefCopy(original: CfirResolvedTypeRef, init: CfirResolvedTypeRefBuilder.() -> Unit): CfirResolvedTypeRef {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirResolvedTypeRefBuilder()
    copyBuilder.coneType = original.coneType
    return copyBuilder.apply(init).build()
}
