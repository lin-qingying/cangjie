

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirSubscriptExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirSubscriptExpressionImpl
import org.cangnova.cangjie.cfir.types.ConeCangjieType

@CfirBuilderDsl
class CfirSubscriptExpressionBuilder {
    var coneTypeOrNull: ConeCangjieType? = null
    lateinit var receiver: CfirExpression
    val indices: MutableList<CfirExpression> = mutableListOf()

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirSubscriptExpression {
        return CfirSubscriptExpressionImpl(
            coneTypeOrNull,
            receiver,
            indices,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildSubscriptExpression(init: CfirSubscriptExpressionBuilder.() -> Unit): CfirSubscriptExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirSubscriptExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildSubscriptExpressionCopy(original: CfirSubscriptExpression, init: CfirSubscriptExpressionBuilder.() -> Unit): CfirSubscriptExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirSubscriptExpressionBuilder()
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.receiver = original.receiver
    copyBuilder.indices.addAll(original.indices)
    return copyBuilder.apply(init).build()
}
