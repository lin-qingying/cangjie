

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.patterns.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.patterns.CfirCommandTypePattern
import org.cangnova.cangjie.cfir.patterns.impl.CfirCommandTypePatternImpl
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirCommandTypePatternBuilder {
    var source: CjSourceElement? = null
    var bindingName: Name? = null
    var isWildcard: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    val typeRefs: MutableList<CfirTypeRef> = mutableListOf()

    fun build(): CfirCommandTypePattern {
        return CfirCommandTypePatternImpl(
            source,
            bindingName,
            isWildcard,
            typeRefs,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildCommandTypePattern(init: CfirCommandTypePatternBuilder.() -> Unit): CfirCommandTypePattern {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirCommandTypePatternBuilder().apply(init).build()
}
