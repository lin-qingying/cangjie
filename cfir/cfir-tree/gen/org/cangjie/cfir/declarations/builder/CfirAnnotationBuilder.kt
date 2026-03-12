/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangjie.cfir.declarations.builder

import kotlin.contracts.*
import org.cangjie.cfir.CfirElement
import org.cangjie.cfir.builder.CfirBuilderDsl
import org.cangjie.cfir.declarations.CfirAnnotation
import org.cangjie.cfir.declarations.impl.CfirAnnotationImpl
import org.cangjie.cfir.types.CfirTypeRef

@CfirBuilderDsl
class CfirAnnotationBuilder {
    lateinit var typeRef: CfirTypeRef
    val arguments: MutableList<CfirElement> = mutableListOf()

    fun build(): CfirAnnotation {
        return CfirAnnotationImpl(
            typeRef,
            arguments,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildAnnotation(init: CfirAnnotationBuilder.() -> Unit): CfirAnnotation {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirAnnotationBuilder().apply(init).build()
}
