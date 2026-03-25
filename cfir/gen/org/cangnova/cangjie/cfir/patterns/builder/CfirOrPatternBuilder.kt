

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.patterns.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.patterns.CfirOrPattern
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.patterns.impl.CfirOrPatternImpl
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirOrPatternBuilder {
    var source: CjSourceElement? = null
    val alternatives: MutableList<CfirPattern> = mutableListOf()

    fun build(): CfirOrPattern {
        return CfirOrPatternImpl(
            source,
            alternatives,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildOrPattern(init: CfirOrPatternBuilder.() -> Unit = {}): CfirOrPattern {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirOrPatternBuilder().apply(init).build()
}
