

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangjie.cfir.CfirImplementationDetail
import org.cangjie.cfir.builder.CfirBuilderDsl
import org.cangjie.cfir.expressions.CfirComparisonExpression
import org.cangjie.cfir.expressions.CfirComparisonOp
import org.cangjie.cfir.expressions.CfirExpression
import org.cangjie.cfir.expressions.impl.CfirComparisonExpressionImpl
import org.cangjie.cfir.types.ConeCangjieType

@CfirBuilderDsl
class CfirComparisonExpressionBuilder {
    var coneTypeOrNull: ConeCangjieType? = null
    lateinit var operation: CfirComparisonOp
    lateinit var left: CfirExpression
    lateinit var right: CfirExpression

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirComparisonExpression {
        return CfirComparisonExpressionImpl(
            coneTypeOrNull,
            operation,
            left,
            right,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildComparisonExpression(init: CfirComparisonExpressionBuilder.() -> Unit): CfirComparisonExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirComparisonExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildComparisonExpressionCopy(original: CfirComparisonExpression, init: CfirComparisonExpressionBuilder.() -> Unit): CfirComparisonExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirComparisonExpressionBuilder()
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.operation = original.operation
    copyBuilder.left = original.left
    copyBuilder.right = original.right
    return copyBuilder.apply(init).build()
}
