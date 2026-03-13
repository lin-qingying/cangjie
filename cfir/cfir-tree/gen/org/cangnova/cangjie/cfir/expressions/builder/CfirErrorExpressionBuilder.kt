

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirErrorExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirErrorExpressionImpl
import org.cangnova.cangjie.cfir.types.ConeCangjieType

@CfirBuilderDsl
class CfirErrorExpressionBuilder {
    var coneTypeOrNull: ConeCangjieType? = null
    lateinit var reason: String

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirErrorExpression {
        return CfirErrorExpressionImpl(
            coneTypeOrNull,
            reason,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildErrorExpression(init: CfirErrorExpressionBuilder.() -> Unit): CfirErrorExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirErrorExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildErrorExpressionCopy(original: CfirErrorExpression, init: CfirErrorExpressionBuilder.() -> Unit): CfirErrorExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirErrorExpressionBuilder()
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.reason = original.reason
    return copyBuilder.apply(init).build()
}
