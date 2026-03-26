

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.patterns.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.patterns.CfirExpressionPattern
import org.cangnova.cangjie.cfir.patterns.impl.CfirExpressionPatternImpl
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirExpressionPatternBuilder {
    var source: CjSourceElement? = null
    lateinit var expression: CfirExpression

    fun build(): CfirExpressionPattern {
        return CfirExpressionPatternImpl(
            source,
            expression,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildExpressionPattern(init: CfirExpressionPatternBuilder.() -> Unit): CfirExpressionPattern {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirExpressionPatternBuilder().apply(init).build()
}
