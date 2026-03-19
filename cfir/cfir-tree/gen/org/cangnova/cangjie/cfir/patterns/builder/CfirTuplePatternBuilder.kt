

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.patterns.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.patterns.CfirTuplePattern
import org.cangnova.cangjie.cfir.patterns.impl.CfirTuplePatternImpl
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirTuplePatternBuilder {
    var source: CjSourceElement? = null
    val elements: MutableList<CfirPattern> = mutableListOf()

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirTuplePattern {
        return CfirTuplePatternImpl(
            source,
            elements,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildTuplePattern(init: CfirTuplePatternBuilder.() -> Unit = {}): CfirTuplePattern {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirTuplePatternBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildTuplePatternCopy(original: CfirTuplePattern, init: CfirTuplePatternBuilder.() -> Unit = {}): CfirTuplePattern {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirTuplePatternBuilder()
    copyBuilder.source = original.source
    copyBuilder.elements.addAll(original.elements)
    return copyBuilder.apply(init).build()
}
