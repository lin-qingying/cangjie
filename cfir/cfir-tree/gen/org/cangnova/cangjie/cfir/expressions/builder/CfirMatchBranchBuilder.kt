

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirMatchBranch
import org.cangnova.cangjie.cfir.expressions.impl.CfirMatchBranchImpl
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.types.ConeCangjieType

@CfirBuilderDsl
class CfirMatchBranchBuilder {
    var coneTypeOrNull: ConeCangjieType? = null
    lateinit var pattern: CfirPattern
    var guard: CfirExpression? = null
    lateinit var body: CfirBlock

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirMatchBranch {
        return CfirMatchBranchImpl(
            coneTypeOrNull,
            pattern,
            guard,
            body,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildMatchBranch(init: CfirMatchBranchBuilder.() -> Unit): CfirMatchBranch {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirMatchBranchBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildMatchBranchCopy(original: CfirMatchBranch, init: CfirMatchBranchBuilder.() -> Unit): CfirMatchBranch {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirMatchBranchBuilder()
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.pattern = original.pattern
    copyBuilder.guard = original.guard
    copyBuilder.body = original.body
    return copyBuilder.apply(init).build()
}
