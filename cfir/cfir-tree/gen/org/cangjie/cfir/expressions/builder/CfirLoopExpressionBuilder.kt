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
import org.cangjie.cfir.expressions.CfirBlock
import org.cangjie.cfir.expressions.CfirExpression
import org.cangjie.cfir.expressions.CfirLoopExpression
import org.cangjie.cfir.expressions.impl.CfirLoopExpressionImpl
import org.cangjie.cfir.types.ConeCangjieType

@CfirBuilderDsl
class CfirLoopExpressionBuilder {
    var coneTypeOrNull: ConeCangjieType? = null
    lateinit var condition: CfirExpression
    lateinit var body: CfirBlock
    var isDoWhile: Boolean by kotlin.properties.Delegates.notNull<Boolean>()

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirLoopExpression {
        return CfirLoopExpressionImpl(
            coneTypeOrNull,
            condition,
            body,
            isDoWhile,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildLoopExpression(init: CfirLoopExpressionBuilder.() -> Unit): CfirLoopExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirLoopExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildLoopExpressionCopy(original: CfirLoopExpression, init: CfirLoopExpressionBuilder.() -> Unit): CfirLoopExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirLoopExpressionBuilder()
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.condition = original.condition
    copyBuilder.body = original.body
    copyBuilder.isDoWhile = original.isDoWhile
    return copyBuilder.apply(init).build()
}
