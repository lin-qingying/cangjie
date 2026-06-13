

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.patterns.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.patterns.CfirCatchPattern
import org.cangnova.cangjie.cfir.patterns.impl.CfirCatchPatternImpl
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirCatchPatternBuilder {
    var source: CjSourceElement? = null
    var bindingName: Name? = null
    var isWildcard: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    val typeRefs: MutableList<CfirTypeRef> = mutableListOf()
    var bindingVariable: CfirPatternBindingVariable? = null

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirCatchPattern {
        return CfirCatchPatternImpl(
            source,
            bindingName,
            isWildcard,
            typeRefs,
            bindingVariable,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildCatchPattern(init: CfirCatchPatternBuilder.() -> Unit): CfirCatchPattern {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirCatchPatternBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildCatchPatternCopy(original: CfirCatchPattern, init: CfirCatchPatternBuilder.() -> Unit): CfirCatchPattern {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirCatchPatternBuilder()
    copyBuilder.source = original.source
    copyBuilder.bindingName = original.bindingName
    copyBuilder.isWildcard = original.isWildcard
    copyBuilder.typeRefs.addAll(original.typeRefs)
    copyBuilder.bindingVariable = original.bindingVariable
    return copyBuilder.apply(init).build()
}
