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
import org.cangjie.cfir.expressions.CfirCatch
import org.cangjie.cfir.expressions.CfirTryExpression
import org.cangjie.cfir.expressions.impl.CfirTryExpressionImpl
import org.cangjie.cfir.types.ConeCangjieType

@CfirBuilderDsl
class CfirTryExpressionBuilder {
    var coneTypeOrNull: ConeCangjieType? = null
    lateinit var tryBlock: CfirBlock
    val catches: MutableList<CfirCatch> = mutableListOf()
    var finallyBlock: CfirBlock? = null

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirTryExpression {
        return CfirTryExpressionImpl(
            coneTypeOrNull,
            tryBlock,
            catches,
            finallyBlock,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildTryExpression(init: CfirTryExpressionBuilder.() -> Unit): CfirTryExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirTryExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildTryExpressionCopy(original: CfirTryExpression, init: CfirTryExpressionBuilder.() -> Unit): CfirTryExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirTryExpressionBuilder()
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.tryBlock = original.tryBlock
    copyBuilder.catches.addAll(original.catches)
    copyBuilder.finallyBlock = original.finallyBlock
    return copyBuilder.apply(init).build()
}
