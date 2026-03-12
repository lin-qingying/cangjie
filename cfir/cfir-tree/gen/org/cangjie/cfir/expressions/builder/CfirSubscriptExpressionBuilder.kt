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
import org.cangjie.cfir.expressions.CfirSubscriptExpression
import org.cangjie.cfir.expressions.impl.CfirSubscriptExpressionImpl
import org.cangjie.cfir.types.ConeCangjieType

@CfirBuilderDsl
class CfirSubscriptExpressionBuilder {
    var coneTypeOrNull: ConeCangjieType? = null
    lateinit var receiver: CfirExpression
    val indices: MutableList<CfirExpression> = mutableListOf()

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirSubscriptExpression {
        return CfirSubscriptExpressionImpl(
            coneTypeOrNull,
            receiver,
            indices,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildSubscriptExpression(init: CfirSubscriptExpressionBuilder.() -> Unit): CfirSubscriptExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirSubscriptExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildSubscriptExpressionCopy(original: CfirSubscriptExpression, init: CfirSubscriptExpressionBuilder.() -> Unit): CfirSubscriptExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirSubscriptExpressionBuilder()
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.receiver = original.receiver
    copyBuilder.indices.addAll(original.indices)
    return copyBuilder.apply(init).build()
}
