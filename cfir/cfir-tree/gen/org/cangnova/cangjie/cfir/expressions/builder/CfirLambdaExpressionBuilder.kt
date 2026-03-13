

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.expressions.CfirLambdaExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirLambdaExpressionImpl
import org.cangnova.cangjie.cfir.types.ConeCangjieType

@CfirBuilderDsl
class CfirLambdaExpressionBuilder {
    var coneTypeOrNull: ConeCangjieType? = null
    lateinit var anonymousFunction: CfirFunction

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirLambdaExpression {
        return CfirLambdaExpressionImpl(
            coneTypeOrNull,
            anonymousFunction,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildLambdaExpression(init: CfirLambdaExpressionBuilder.() -> Unit): CfirLambdaExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirLambdaExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildLambdaExpressionCopy(original: CfirLambdaExpression, init: CfirLambdaExpressionBuilder.() -> Unit): CfirLambdaExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirLambdaExpressionBuilder()
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.anonymousFunction = original.anonymousFunction
    return copyBuilder.apply(init).build()
}
