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
import org.cangjie.cfir.expressions.CfirAssignment
import org.cangjie.cfir.expressions.CfirExpression
import org.cangjie.cfir.expressions.impl.CfirAssignmentImpl
import org.cangjie.cfir.types.ConeCangjieType

@CfirBuilderDsl
class CfirAssignmentBuilder {
    var coneTypeOrNull: ConeCangjieType? = null
    lateinit var lValue: CfirExpression
    lateinit var rValue: CfirExpression

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirAssignment {
        return CfirAssignmentImpl(
            coneTypeOrNull,
            lValue,
            rValue,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildAssignment(init: CfirAssignmentBuilder.() -> Unit): CfirAssignment {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirAssignmentBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildAssignmentCopy(original: CfirAssignment, init: CfirAssignmentBuilder.() -> Unit): CfirAssignment {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirAssignmentBuilder()
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.lValue = original.lValue
    copyBuilder.rValue = original.rValue
    return copyBuilder.apply(init).build()
}
