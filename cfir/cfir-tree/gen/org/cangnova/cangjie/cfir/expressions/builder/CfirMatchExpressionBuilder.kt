

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirMatchBranch
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirMatchExpressionImpl
import org.cangnova.cangjie.cfir.types.ConeCangjieType

@CfirBuilderDsl
class CfirMatchExpressionBuilder {
    var coneTypeOrNull: ConeCangjieType? = null
    lateinit var subject: CfirExpression
    val branches: MutableList<CfirMatchBranch> = mutableListOf()

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirMatchExpression {
        return CfirMatchExpressionImpl(
            coneTypeOrNull,
            subject,
            branches,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildMatchExpression(init: CfirMatchExpressionBuilder.() -> Unit): CfirMatchExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirMatchExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildMatchExpressionCopy(original: CfirMatchExpression, init: CfirMatchExpressionBuilder.() -> Unit): CfirMatchExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirMatchExpressionBuilder()
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.subject = original.subject
    copyBuilder.branches.addAll(original.branches)
    return copyBuilder.apply(init).build()
}
