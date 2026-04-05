

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirArgumentList
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirArgumentListImpl
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirArgumentListBuilder {
    var source: CjSourceElement? = null
    val arguments: MutableList<CfirExpression> = mutableListOf()

    fun build(): CfirArgumentList {
        return CfirArgumentListImpl(
            source,
            arguments,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildArgumentList(init: CfirArgumentListBuilder.() -> Unit = {}): CfirArgumentList {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirArgumentListBuilder().apply(init).build()
}
