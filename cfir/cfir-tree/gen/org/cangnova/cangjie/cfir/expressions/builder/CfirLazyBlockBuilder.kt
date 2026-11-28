

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.annotations.CangjieOverflowStrategy
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLazyBlock
import org.cangnova.cangjie.cfir.expressions.impl.CfirLazyBlockImpl

@CfirBuilderDsl
class CfirLazyBlockBuilder {
    var mappedExpression: CfirExpression? = null
    var overflowStrategy: CangjieOverflowStrategy? = null

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirLazyBlock {
        return CfirLazyBlockImpl(
            mappedExpression,
            overflowStrategy,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildLazyBlock(init: CfirLazyBlockBuilder.() -> Unit = {}): CfirLazyBlock {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirLazyBlockBuilder().apply(init).build()
}
