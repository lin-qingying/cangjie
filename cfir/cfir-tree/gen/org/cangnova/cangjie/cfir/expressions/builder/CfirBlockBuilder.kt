

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.impl.CfirBlockImpl
import org.cangnova.cangjie.cfir.types.ConeCangjieType

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
