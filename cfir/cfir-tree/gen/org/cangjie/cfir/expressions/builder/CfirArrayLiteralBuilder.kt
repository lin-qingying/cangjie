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
import org.cangjie.cfir.expressions.CfirArrayLiteral
import org.cangjie.cfir.expressions.CfirExpression
import org.cangjie.cfir.expressions.impl.CfirArrayLiteralImpl
import org.cangjie.cfir.types.ConeCangjieType

@CfirBuilderDsl
class CfirArrayLiteralBuilder {
    var coneTypeOrNull: ConeCangjieType? = null
    val elements: MutableList<CfirExpression> = mutableListOf()

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirArrayLiteral {
        return CfirArrayLiteralImpl(
            coneTypeOrNull,
            elements,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildArrayLiteral(init: CfirArrayLiteralBuilder.() -> Unit = {}): CfirArrayLiteral {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirArrayLiteralBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildArrayLiteralCopy(original: CfirArrayLiteral, init: CfirArrayLiteralBuilder.() -> Unit = {}): CfirArrayLiteral {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirArrayLiteralBuilder()
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.elements.addAll(original.elements)
    return copyBuilder.apply(init).build()
}
