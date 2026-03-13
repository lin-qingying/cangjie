

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangjie.cfir.CfirImplementationDetail
import org.cangjie.cfir.builder.CfirBuilderDsl
import org.cangjie.cfir.expressions.CfirExpression
import org.cangjie.cfir.expressions.CfirRangeExpression
import org.cangjie.cfir.expressions.impl.CfirRangeExpressionImpl
import org.cangjie.cfir.types.ConeCangjieType

@CfirBuilderDsl
class CfirRangeExpressionBuilder {
    var coneTypeOrNull: ConeCangjieType? = null
    lateinit var start: CfirExpression
    lateinit var end: CfirExpression
    var isInclusive: Boolean by kotlin.properties.Delegates.notNull<Boolean>()

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirRangeExpression {
        return CfirRangeExpressionImpl(
            coneTypeOrNull,
            start,
            end,
            isInclusive,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildRangeExpression(init: CfirRangeExpressionBuilder.() -> Unit): CfirRangeExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirRangeExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildRangeExpressionCopy(original: CfirRangeExpression, init: CfirRangeExpressionBuilder.() -> Unit): CfirRangeExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirRangeExpressionBuilder()
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.start = original.start
    copyBuilder.end = original.end
    copyBuilder.isInclusive = original.isInclusive
    return copyBuilder.apply(init).build()
}
