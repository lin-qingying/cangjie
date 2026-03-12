/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangjie.cfir.CfirElement
import org.cangjie.cfir.CfirImplementationDetail
import org.cangjie.cfir.builder.CfirBuilderDsl
import org.cangjie.cfir.expressions.CfirBlock
import org.cangjie.cfir.expressions.impl.CfirBlockImpl
import org.cangjie.cfir.types.ConeCangjieType

@CfirBuilderDsl
class CfirBlockBuilder {
    var coneTypeOrNull: ConeCangjieType? = null
    val statements: MutableList<CfirElement> = mutableListOf()

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirBlock {
        return CfirBlockImpl(
            coneTypeOrNull,
            statements,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildBlock(init: CfirBlockBuilder.() -> Unit = {}): CfirBlock {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirBlockBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildBlockCopy(original: CfirBlock, init: CfirBlockBuilder.() -> Unit = {}): CfirBlock {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirBlockBuilder()
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.statements.addAll(original.statements)
    return copyBuilder.apply(init).build()
}
