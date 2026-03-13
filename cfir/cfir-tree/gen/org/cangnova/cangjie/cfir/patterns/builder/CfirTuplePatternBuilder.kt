

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.patterns.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.patterns.CfirTuplePattern
import org.cangnova.cangjie.cfir.patterns.impl.CfirTuplePatternImpl

@CfirBuilderDsl
class CfirTuplePatternBuilder {
    val elements: MutableList<CfirPattern> = mutableListOf()

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirTuplePattern {
        return CfirTuplePatternImpl(
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
    copyBuilder.elements.addAll(original.elements)
    return copyBuilder.apply(init).build()
}
