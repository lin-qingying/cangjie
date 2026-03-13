

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangjie.cfir.patterns.builder

import kotlin.contracts.*
import org.cangjie.cfir.CfirImplementationDetail
import org.cangjie.cfir.builder.CfirBuilderDsl
import org.cangjie.cfir.expressions.CfirExpression
import org.cangjie.cfir.patterns.CfirConstPattern
import org.cangjie.cfir.patterns.impl.CfirConstPatternImpl

@CfirBuilderDsl
class CfirConstPatternBuilder {
    lateinit var expression: CfirExpression

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirConstPattern {
        return CfirConstPatternImpl(
            expression,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildConstPattern(init: CfirConstPatternBuilder.() -> Unit): CfirConstPattern {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirConstPatternBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildConstPatternCopy(original: CfirConstPattern, init: CfirConstPatternBuilder.() -> Unit): CfirConstPattern {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirConstPatternBuilder()
    copyBuilder.expression = original.expression
    return copyBuilder.apply(init).build()
}
