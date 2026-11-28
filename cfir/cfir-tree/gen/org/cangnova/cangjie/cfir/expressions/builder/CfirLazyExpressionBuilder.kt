

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.annotations.CangjieOverflowStrategy
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLazyExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirLazyExpressionImpl
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirLazyExpressionBuilder {
    var source: CjSourceElement? = null
    var mappedExpression: CfirExpression? = null
    var overflowStrategy: CangjieOverflowStrategy? = null

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirLazyExpression {
        return CfirLazyExpressionImpl(
            source,
            mappedExpression,
            overflowStrategy,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildLazyExpression(init: CfirLazyExpressionBuilder.() -> Unit = {}): CfirLazyExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirLazyExpressionBuilder().apply(init).build()
}
