

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.patterns.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.patterns.CfirConstPattern
import org.cangnova.cangjie.cfir.patterns.impl.CfirConstPatternImpl
import org.cangnova.cangjie.cfir.source.CjSourceElement

@CfirBuilderDsl
class CfirConstPatternBuilder {
    var source: CjSourceElement? = null
    lateinit var expression: CfirExpression

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirConstPattern {
        return CfirConstPatternImpl(
            source,
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
    copyBuilder.source = original.source
    copyBuilder.expression = original.expression
    return copyBuilder.apply(init).build()
}
