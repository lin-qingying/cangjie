

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirReturnExpressionImpl
import org.cangnova.cangjie.cfir.types.ConeCangjieType

@CfirBuilderDsl
class CfirReturnExpressionBuilder {
    var coneTypeOrNull: ConeCangjieType? = null
    var result: CfirExpression? = null

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirReturnExpression {
        return CfirReturnExpressionImpl(
            coneTypeOrNull,
            result,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildReturnExpression(init: CfirReturnExpressionBuilder.() -> Unit = {}): CfirReturnExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirReturnExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildReturnExpressionCopy(original: CfirReturnExpression, init: CfirReturnExpressionBuilder.() -> Unit = {}): CfirReturnExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirReturnExpressionBuilder()
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.result = original.result
    return copyBuilder.apply(init).build()
}
