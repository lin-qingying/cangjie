

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLoopExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirLoopExpressionImpl
import org.cangnova.cangjie.cfir.types.ConeCangjieType

@CfirBuilderDsl
class CfirLoopExpressionBuilder {
    var coneTypeOrNull: ConeCangjieType? = null
    lateinit var condition: CfirExpression
    lateinit var body: CfirBlock
    var isDoWhile: Boolean by kotlin.properties.Delegates.notNull<Boolean>()

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirLoopExpression {
        return CfirLoopExpressionImpl(
            coneTypeOrNull,
            condition,
            body,
            isDoWhile,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildLoopExpression(init: CfirLoopExpressionBuilder.() -> Unit): CfirLoopExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirLoopExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildLoopExpressionCopy(original: CfirLoopExpression, init: CfirLoopExpressionBuilder.() -> Unit): CfirLoopExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirLoopExpressionBuilder()
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.condition = original.condition
    copyBuilder.body = original.body
    copyBuilder.isDoWhile = original.isDoWhile
    return copyBuilder.apply(init).build()
}
