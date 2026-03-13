

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangjie.cfir.patterns.builder

import kotlin.contracts.*
import org.cangjie.cfir.CfirImplementationDetail
import org.cangjie.cfir.builder.CfirBuilderDsl
import org.cangjie.cfir.patterns.CfirTypePattern
import org.cangjie.cfir.patterns.impl.CfirTypePatternImpl
import org.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.name.Name

@CfirBuilderDsl
class CfirTypePatternBuilder {
    lateinit var typeRef: CfirTypeRef
    var bindingName: Name? = null

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirTypePattern {
        return CfirTypePatternImpl(
            typeRef,
            bindingName,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildTypePattern(init: CfirTypePatternBuilder.() -> Unit): CfirTypePattern {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirTypePatternBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildTypePatternCopy(original: CfirTypePattern, init: CfirTypePatternBuilder.() -> Unit): CfirTypePattern {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirTypePatternBuilder()
    copyBuilder.typeRef = original.typeRef
    copyBuilder.bindingName = original.bindingName
    return copyBuilder.apply(init).build()
}
