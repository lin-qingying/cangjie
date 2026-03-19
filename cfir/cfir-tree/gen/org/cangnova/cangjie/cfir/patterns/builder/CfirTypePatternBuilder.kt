

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.patterns.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.patterns.CfirTypePattern
import org.cangnova.cangjie.cfir.patterns.impl.CfirTypePatternImpl
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirTypePatternBuilder {
    var source: CjSourceElement? = null
    lateinit var typeRef: CfirTypeRef
    var bindingName: Name? = null

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirTypePattern {
        return CfirTypePatternImpl(
            source,
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
    copyBuilder.source = original.source
    copyBuilder.typeRef = original.typeRef
    copyBuilder.bindingName = original.bindingName
    return copyBuilder.apply(init).build()
}
