

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirForInExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirForInExpressionImpl
import org.cangnova.cangjie.cfir.types.ConeCangjieType

@CfirBuilderDsl
class CfirForInExpressionBuilder {
    var coneTypeOrNull: ConeCangjieType? = null
    lateinit var condition: CfirExpression
    var isDoWhile: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    lateinit var variable: CfirVariable
    lateinit var iterable: CfirExpression
    lateinit var body: CfirBlock

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirForInExpression {
        return CfirForInExpressionImpl(
            coneTypeOrNull,
            condition,
            isDoWhile,
            variable,
            iterable,
            body,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildForInExpression(init: CfirForInExpressionBuilder.() -> Unit): CfirForInExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirForInExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildForInExpressionCopy(original: CfirForInExpression, init: CfirForInExpressionBuilder.() -> Unit): CfirForInExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirForInExpressionBuilder()
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.condition = original.condition
    copyBuilder.isDoWhile = original.isDoWhile
    copyBuilder.variable = original.variable
    copyBuilder.iterable = original.iterable
    copyBuilder.body = original.body
    return copyBuilder.apply(init).build()
}
