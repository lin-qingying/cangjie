

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangjie.cfir.CfirImplementationDetail
import org.cangjie.cfir.builder.CfirBuilderDsl
import org.cangjie.cfir.expressions.CfirBlock
import org.cangjie.cfir.expressions.CfirSpawnExpression
import org.cangjie.cfir.expressions.impl.CfirSpawnExpressionImpl
import org.cangjie.cfir.types.ConeCangjieType

@CfirBuilderDsl
class CfirSpawnExpressionBuilder {
    var coneTypeOrNull: ConeCangjieType? = null
    lateinit var body: CfirBlock

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirSpawnExpression {
        return CfirSpawnExpressionImpl(
            coneTypeOrNull,
            body,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildSpawnExpression(init: CfirSpawnExpressionBuilder.() -> Unit): CfirSpawnExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirSpawnExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildSpawnExpressionCopy(original: CfirSpawnExpression, init: CfirSpawnExpressionBuilder.() -> Unit): CfirSpawnExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirSpawnExpressionBuilder()
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.body = original.body
    return copyBuilder.apply(init).build()
}
