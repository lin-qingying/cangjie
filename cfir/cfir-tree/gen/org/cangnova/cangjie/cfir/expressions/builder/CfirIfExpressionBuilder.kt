

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirIfExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirIfExpressionImpl
import org.cangnova.cangjie.cfir.types.ConeCangjieType

@CfirBuilderDsl
class CfirIfExpressionBuilder {
    var coneTypeOrNull: ConeCangjieType? = null
    lateinit var condition: CfirExpression
    lateinit var thenBranch: CfirBlock
    var elseBranch: CfirExpression? = null

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirIfExpression {
        return CfirIfExpressionImpl(
            coneTypeOrNull,
            condition,
            thenBranch,
            elseBranch,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildIfExpression(init: CfirIfExpressionBuilder.() -> Unit): CfirIfExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirIfExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildIfExpressionCopy(original: CfirIfExpression, init: CfirIfExpressionBuilder.() -> Unit): CfirIfExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirIfExpressionBuilder()
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.condition = original.condition
    copyBuilder.thenBranch = original.thenBranch
    copyBuilder.elseBranch = original.elseBranch
    return copyBuilder.apply(init).build()
}
