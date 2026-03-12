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
import org.cangjie.cfir.expressions.CfirFunctionCall
import org.cangjie.cfir.expressions.impl.CfirFunctionCallImpl
import org.cangjie.cfir.references.CfirReference
import org.cangjie.cfir.types.CfirTypeRef
import org.cangjie.cfir.types.ConeCangjieType

@CfirBuilderDsl
class CfirFunctionCallBuilder {
    var coneTypeOrNull: ConeCangjieType? = null
    lateinit var calleeReference: CfirReference
    var explicitReceiver: CfirExpression? = null
    val arguments: MutableList<CfirExpression> = mutableListOf()
    val typeArguments: MutableList<CfirTypeRef> = mutableListOf()

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirFunctionCall {
        return CfirFunctionCallImpl(
            coneTypeOrNull,
            calleeReference,
            explicitReceiver,
            arguments,
            typeArguments,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildFunctionCall(init: CfirFunctionCallBuilder.() -> Unit): CfirFunctionCall {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirFunctionCallBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildFunctionCallCopy(original: CfirFunctionCall, init: CfirFunctionCallBuilder.() -> Unit): CfirFunctionCall {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirFunctionCallBuilder()
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.calleeReference = original.calleeReference
    copyBuilder.explicitReceiver = original.explicitReceiver
    copyBuilder.arguments.addAll(original.arguments)
    copyBuilder.typeArguments.addAll(original.typeArguments)
    return copyBuilder.apply(init).build()
}
