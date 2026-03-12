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
import org.cangjie.cfir.declarations.CfirVariable
import org.cangjie.cfir.expressions.CfirBlock
import org.cangjie.cfir.expressions.CfirExpression
import org.cangjie.cfir.expressions.CfirForInExpression
import org.cangjie.cfir.expressions.impl.CfirForInExpressionImpl
import org.cangjie.cfir.types.ConeCangjieType

@CfirBuilderDsl
class CfirForInExpressionBuilder {
    var coneTypeOrNull: ConeCangjieType? = null
    lateinit var condition: CfirExpression
    var isDoWhile: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    lateinit var variable: CfirVariable
    lateinit var iterable: CfirExpression
    lateinit var body: CfirBlock

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirForInExpression {
        return CfirForInExpressionImpl(
            coneTypeOrNull,
            condition,
            isDoWhile,
            variable,
            iterable,
            body,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildForInExpression(init: CfirForInExpressionBuilder.() -> Unit): CfirForInExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirForInExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildForInExpressionCopy(original: CfirForInExpression, init: CfirForInExpressionBuilder.() -> Unit): CfirForInExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirForInExpressionBuilder()
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.condition = original.condition
    copyBuilder.isDoWhile = original.isDoWhile
    copyBuilder.variable = original.variable
    copyBuilder.iterable = original.iterable
    copyBuilder.body = original.body
    return copyBuilder.apply(init).build()
}
