

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.types.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.types.CfirOptionTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.impl.CfirOptionTypeRefImpl
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirOptionTypeRefBuilder {
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var customRenderer: Boolean = false
    lateinit var source: CjSourceElement
    lateinit var componentTypeRef: CfirTypeRef

    fun build(): CfirOptionTypeRef {
        return CfirOptionTypeRefImpl(
            annotations.toMutableOrEmpty(),
            customRenderer,
            source,
            componentTypeRef,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildOptionTypeRef(init: CfirOptionTypeRefBuilder.() -> Unit): CfirOptionTypeRef {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirOptionTypeRefBuilder().apply(init).build()
}
