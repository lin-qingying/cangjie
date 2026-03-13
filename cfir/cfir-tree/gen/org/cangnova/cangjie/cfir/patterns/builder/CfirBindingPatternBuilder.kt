

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangjie.cfir.patterns.builder

import kotlin.contracts.*
import org.cangjie.cfir.CfirImplementationDetail
import org.cangjie.cfir.builder.CfirBuilderDsl
import org.cangjie.cfir.patterns.CfirBindingPattern
import org.cangjie.cfir.patterns.CfirPattern
import org.cangjie.cfir.patterns.impl.CfirBindingPatternImpl
import org.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.name.Name

@CfirBuilderDsl
class CfirBindingPatternBuilder {
    lateinit var name: Name
    var typeRef: CfirTypeRef? = null
    var nestedPattern: CfirPattern? = null

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirBindingPattern {
        return CfirBindingPatternImpl(
            name,
            typeRef,
            nestedPattern,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildBindingPattern(init: CfirBindingPatternBuilder.() -> Unit): CfirBindingPattern {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirBindingPatternBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildBindingPatternCopy(original: CfirBindingPattern, init: CfirBindingPatternBuilder.() -> Unit): CfirBindingPattern {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirBindingPatternBuilder()
    copyBuilder.name = original.name
    copyBuilder.typeRef = original.typeRef
    copyBuilder.nestedPattern = original.nestedPattern
    return copyBuilder.apply(init).build()
}
