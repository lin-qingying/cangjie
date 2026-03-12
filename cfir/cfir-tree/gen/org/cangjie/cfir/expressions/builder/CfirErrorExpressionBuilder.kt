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
import org.cangjie.cfir.expressions.CfirErrorExpression
import org.cangjie.cfir.expressions.impl.CfirErrorExpressionImpl
import org.cangjie.cfir.types.ConeCangjieType

@CfirBuilderDsl
class CfirErrorExpressionBuilder {
    var coneTypeOrNull: ConeCangjieType? = null
    lateinit var reason: String

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirErrorExpression {
        return CfirErrorExpressionImpl(
            coneTypeOrNull,
            reason,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildErrorExpression(init: CfirErrorExpressionBuilder.() -> Unit): CfirErrorExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirErrorExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildErrorExpressionCopy(original: CfirErrorExpression, init: CfirErrorExpressionBuilder.() -> Unit): CfirErrorExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirErrorExpressionBuilder()
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.reason = original.reason
    return copyBuilder.apply(init).build()
}
