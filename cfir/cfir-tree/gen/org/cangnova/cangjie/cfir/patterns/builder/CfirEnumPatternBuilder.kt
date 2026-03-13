

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.patterns.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.patterns.CfirEnumPattern
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.patterns.impl.CfirEnumPatternImpl
import org.cangnova.cangjie.cfir.references.CfirReference

@CfirBuilderDsl
class CfirEnumPatternBuilder {
    lateinit var constructorReference: CfirReference
    val arguments: MutableList<CfirPattern> = mutableListOf()

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirEnumPattern {
        return CfirEnumPatternImpl(
            constructorReference,
            arguments,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildEnumPattern(init: CfirEnumPatternBuilder.() -> Unit): CfirEnumPattern {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirEnumPatternBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildEnumPatternCopy(original: CfirEnumPattern, init: CfirEnumPatternBuilder.() -> Unit): CfirEnumPattern {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirEnumPatternBuilder()
    copyBuilder.constructorReference = original.constructorReference
    copyBuilder.arguments.addAll(original.arguments)
    return copyBuilder.apply(init).build()
}
