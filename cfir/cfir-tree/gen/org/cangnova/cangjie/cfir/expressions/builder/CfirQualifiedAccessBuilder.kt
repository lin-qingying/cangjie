

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccess
import org.cangnova.cangjie.cfir.expressions.impl.CfirQualifiedAccessImpl
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangjieType

@CfirBuilderDsl
class CfirQualifiedAccessBuilder {
    var coneTypeOrNull: ConeCangjieType? = null
    lateinit var calleeReference: CfirReference
    var explicitReceiver: CfirExpression? = null
    val typeArguments: MutableList<CfirTypeRef> = mutableListOf()

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirQualifiedAccess {
        return CfirQualifiedAccessImpl(
            coneTypeOrNull,
            calleeReference,
            explicitReceiver,
            typeArguments,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildQualifiedAccess(init: CfirQualifiedAccessBuilder.() -> Unit): CfirQualifiedAccess {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirQualifiedAccessBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildQualifiedAccessCopy(original: CfirQualifiedAccess, init: CfirQualifiedAccessBuilder.() -> Unit): CfirQualifiedAccess {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirQualifiedAccessBuilder()
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.calleeReference = original.calleeReference
    copyBuilder.explicitReceiver = original.explicitReceiver
    copyBuilder.typeArguments.addAll(original.typeArguments)
    return copyBuilder.apply(init).build()
}
