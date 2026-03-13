

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirArrayLiteral
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirArrayLiteralImpl
import org.cangnova.cangjie.cfir.types.ConeCangjieType

@CfirBuilderDsl
class CfirArrayLiteralBuilder {
    var coneTypeOrNull: ConeCangjieType? = null
    val elements: MutableList<CfirExpression> = mutableListOf()

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirArrayLiteral {
        return CfirArrayLiteralImpl(
            coneTypeOrNull,
            elements,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildArrayLiteral(init: CfirArrayLiteralBuilder.() -> Unit = {}): CfirArrayLiteral {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirArrayLiteralBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildArrayLiteralCopy(original: CfirArrayLiteral, init: CfirArrayLiteralBuilder.() -> Unit = {}): CfirArrayLiteral {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirArrayLiteralBuilder()
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.elements.addAll(original.elements)
    return copyBuilder.apply(init).build()
}
