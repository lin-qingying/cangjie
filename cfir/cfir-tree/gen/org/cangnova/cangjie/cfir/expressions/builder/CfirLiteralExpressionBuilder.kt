

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangjie.cfir.CfirImplementationDetail
import org.cangjie.cfir.builder.CfirBuilderDsl
import org.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangjie.cfir.expressions.CfirLiteralKind
import org.cangjie.cfir.expressions.impl.CfirLiteralExpressionImpl
import org.cangjie.cfir.types.ConeCangjieType

@CfirBuilderDsl
class CfirLiteralExpressionBuilder {
    var coneTypeOrNull: ConeCangjieType? = null
    lateinit var kind: CfirLiteralKind
    var value: Any? = null

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirLiteralExpression {
        return CfirLiteralExpressionImpl(
            coneTypeOrNull,
            kind,
            value,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildLiteralExpression(init: CfirLiteralExpressionBuilder.() -> Unit): CfirLiteralExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirLiteralExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildLiteralExpressionCopy(original: CfirLiteralExpression, init: CfirLiteralExpressionBuilder.() -> Unit): CfirLiteralExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirLiteralExpressionBuilder()
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.kind = original.kind
    copyBuilder.value = original.value
    return copyBuilder.apply(init).build()
}
