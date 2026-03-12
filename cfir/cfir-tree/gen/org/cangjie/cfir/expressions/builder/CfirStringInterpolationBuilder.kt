/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangjie.cfir.CfirImplementationDetail
import org.cangjie.cfir.builder.CfirBuilderDsl
import org.cangjie.cfir.expressions.CfirExpression
import org.cangjie.cfir.expressions.CfirStringInterpolation
import org.cangjie.cfir.expressions.impl.CfirStringInterpolationImpl
import org.cangjie.cfir.types.ConeCangjieType

@CfirBuilderDsl
class CfirStringInterpolationBuilder {
    var coneTypeOrNull: ConeCangjieType? = null
    val parts: MutableList<CfirExpression> = mutableListOf()

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirStringInterpolation {
        return CfirStringInterpolationImpl(
            coneTypeOrNull,
            parts,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildStringInterpolation(init: CfirStringInterpolationBuilder.() -> Unit = {}): CfirStringInterpolation {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirStringInterpolationBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildStringInterpolationCopy(original: CfirStringInterpolation, init: CfirStringInterpolationBuilder.() -> Unit = {}): CfirStringInterpolation {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirStringInterpolationBuilder()
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.parts.addAll(original.parts)
    return copyBuilder.apply(init).build()
}
