

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirThrowExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirThrowExpressionImpl
import org.cangnova.cangjie.cfir.types.ConeCangjieType

@CfirBuilderDsl
class CfirThrowExpressionBuilder {
    var coneTypeOrNull: ConeCangjieType? = null
    lateinit var exception: CfirExpression

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirThrowExpression {
        return CfirThrowExpressionImpl(
            coneTypeOrNull,
            exception,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildThrowExpression(init: CfirThrowExpressionBuilder.() -> Unit): CfirThrowExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirThrowExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildThrowExpressionCopy(original: CfirThrowExpression, init: CfirThrowExpressionBuilder.() -> Unit): CfirThrowExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirThrowExpressionBuilder()
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.exception = original.exception
    return copyBuilder.apply(init).build()
}
