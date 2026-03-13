

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirTupleLiteral
import org.cangnova.cangjie.cfir.expressions.impl.CfirTupleLiteralImpl
import org.cangnova.cangjie.cfir.types.ConeCangjieType

@CfirBuilderDsl
class CfirTupleLiteralBuilder {
    var coneTypeOrNull: ConeCangjieType? = null
    val elements: MutableList<CfirExpression> = mutableListOf()

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirTupleLiteral {
        return CfirTupleLiteralImpl(
            coneTypeOrNull,
            elements,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildTupleLiteral(init: CfirTupleLiteralBuilder.() -> Unit = {}): CfirTupleLiteral {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirTupleLiteralBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildTupleLiteralCopy(original: CfirTupleLiteral, init: CfirTupleLiteralBuilder.() -> Unit = {}): CfirTupleLiteral {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirTupleLiteralBuilder()
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.elements.addAll(original.elements)
    return copyBuilder.apply(init).build()
}
