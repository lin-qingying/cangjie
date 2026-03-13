

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangjie.cfir.CfirImplementationDetail
import org.cangjie.cfir.builder.CfirBuilderDsl
import org.cangjie.cfir.expressions.CfirExpression
import org.cangjie.cfir.expressions.CfirPropertyAccess
import org.cangjie.cfir.expressions.impl.CfirPropertyAccessImpl
import org.cangjie.cfir.references.CfirReference
import org.cangjie.cfir.types.ConeCangjieType

@CfirBuilderDsl
class CfirPropertyAccessBuilder {
    var coneTypeOrNull: ConeCangjieType? = null
    lateinit var calleeReference: CfirReference
    var explicitReceiver: CfirExpression? = null

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirPropertyAccess {
        return CfirPropertyAccessImpl(
            coneTypeOrNull,
            calleeReference,
            explicitReceiver,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildPropertyAccess(init: CfirPropertyAccessBuilder.() -> Unit): CfirPropertyAccess {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirPropertyAccessBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildPropertyAccessCopy(original: CfirPropertyAccess, init: CfirPropertyAccessBuilder.() -> Unit): CfirPropertyAccess {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirPropertyAccessBuilder()
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.calleeReference = original.calleeReference
    copyBuilder.explicitReceiver = original.explicitReceiver
    return copyBuilder.apply(init).build()
}
