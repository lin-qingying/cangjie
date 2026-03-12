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
import org.cangjie.cfir.expressions.CfirTypeOperationKind
import org.cangjie.cfir.expressions.CfirTypeOperator
import org.cangjie.cfir.expressions.impl.CfirTypeOperatorImpl
import org.cangjie.cfir.types.CfirTypeRef
import org.cangjie.cfir.types.ConeCangjieType

@CfirBuilderDsl
class CfirTypeOperatorBuilder {
    var coneTypeOrNull: ConeCangjieType? = null
    lateinit var operation: CfirTypeOperationKind
    lateinit var argument: CfirExpression
    lateinit var typeRef: CfirTypeRef

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirTypeOperator {
        return CfirTypeOperatorImpl(
            coneTypeOrNull,
            operation,
            argument,
            typeRef,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildTypeOperator(init: CfirTypeOperatorBuilder.() -> Unit): CfirTypeOperator {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirTypeOperatorBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildTypeOperatorCopy(original: CfirTypeOperator, init: CfirTypeOperatorBuilder.() -> Unit): CfirTypeOperator {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirTypeOperatorBuilder()
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.operation = original.operation
    copyBuilder.argument = original.argument
    copyBuilder.typeRef = original.typeRef
    return copyBuilder.apply(init).build()
}
