

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirJumpExpression
import org.cangnova.cangjie.cfir.expressions.CfirJumpKind
import org.cangnova.cangjie.cfir.expressions.impl.CfirJumpExpressionImpl
import org.cangnova.cangjie.cfir.types.ConeCangjieType

@CfirBuilderDsl
class CfirJumpExpressionBuilder {
    var coneTypeOrNull: ConeCangjieType? = null
    lateinit var kind: CfirJumpKind

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirJumpExpression {
        return CfirJumpExpressionImpl(
            coneTypeOrNull,
            kind,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildJumpExpression(init: CfirJumpExpressionBuilder.() -> Unit): CfirJumpExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirJumpExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildJumpExpressionCopy(original: CfirJumpExpression, init: CfirJumpExpressionBuilder.() -> Unit): CfirJumpExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirJumpExpressionBuilder()
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.kind = original.kind
    return copyBuilder.apply(init).build()
}
