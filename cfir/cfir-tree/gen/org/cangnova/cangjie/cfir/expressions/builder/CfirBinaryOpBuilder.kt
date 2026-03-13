

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirBinaryOp
import org.cangnova.cangjie.cfir.expressions.CfirBinaryOpKind
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirBinaryOpImpl
import org.cangnova.cangjie.cfir.types.ConeCangjieType

@CfirBuilderDsl
class CfirBinaryOpBuilder {
    var coneTypeOrNull: ConeCangjieType? = null
    lateinit var kind: CfirBinaryOpKind
    lateinit var left: CfirExpression
    lateinit var right: CfirExpression

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirBinaryOp {
        return CfirBinaryOpImpl(
            coneTypeOrNull,
            kind,
            left,
            right,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildBinaryOp(init: CfirBinaryOpBuilder.() -> Unit): CfirBinaryOp {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirBinaryOpBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildBinaryOpCopy(original: CfirBinaryOp, init: CfirBinaryOpBuilder.() -> Unit): CfirBinaryOp {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirBinaryOpBuilder()
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.kind = original.kind
    copyBuilder.left = original.left
    copyBuilder.right = original.right
    return copyBuilder.apply(init).build()
}
