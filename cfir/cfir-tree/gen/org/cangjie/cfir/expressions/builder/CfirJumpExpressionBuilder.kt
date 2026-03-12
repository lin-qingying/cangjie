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
import org.cangjie.cfir.expressions.CfirJumpExpression
import org.cangjie.cfir.expressions.CfirJumpKind
import org.cangjie.cfir.expressions.impl.CfirJumpExpressionImpl
import org.cangjie.cfir.types.ConeCangjieType

@CfirBuilderDsl
class CfirJumpExpressionBuilder {
    var coneTypeOrNull: ConeCangjieType? = null
    lateinit var kind: CfirJumpKind

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirJumpExpression {
        return CfirJumpExpressionImpl(
            coneTypeOrNull,
            kind,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildJumpExpression(init: CfirJumpExpressionBuilder.() -> Unit): CfirJumpExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirJumpExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildJumpExpressionCopy(original: CfirJumpExpression, init: CfirJumpExpressionBuilder.() -> Unit): CfirJumpExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirJumpExpressionBuilder()
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.kind = original.kind
    return copyBuilder.apply(init).build()
}
