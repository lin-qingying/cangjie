

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirAssignmentImpl
import org.cangnova.cangjie.cfir.types.ConeCangjieType

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
