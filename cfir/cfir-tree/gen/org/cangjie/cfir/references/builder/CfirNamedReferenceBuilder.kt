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
import org.cangjie.cfir.references.CfirNamedReference
import org.cangjie.cfir.references.impl.CfirNamedReferenceImpl
import org.cangnova.cangjie.name.Name

@CfirBuilderDsl
class CfirNamedReferenceBuilder {
    lateinit var name: Name

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirNamedReference {
        return CfirNamedReferenceImpl(
            name,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildNamedReference(init: CfirNamedReferenceBuilder.() -> Unit): CfirNamedReference {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirNamedReferenceBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildNamedReferenceCopy(original: CfirNamedReference, init: CfirNamedReferenceBuilder.() -> Unit): CfirNamedReference {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirNamedReferenceBuilder()
    copyBuilder.name = original.name
    return copyBuilder.apply(init).build()
}
