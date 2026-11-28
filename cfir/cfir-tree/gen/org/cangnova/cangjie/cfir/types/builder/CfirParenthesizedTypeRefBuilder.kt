

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.types.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.types.CfirParenthesizedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRefOrigin
import org.cangnova.cangjie.cfir.types.impl.CfirParenthesizedTypeRefImpl
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirParenthesizedTypeRefBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var customRenderer: Boolean = false
    var typeUseOrigin: CfirTypeRefOrigin = CfirTypeRefOrigin.SEMANTIC
    lateinit var innerTypeRef: CfirTypeRef

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirParenthesizedTypeRef {
        return CfirParenthesizedTypeRefImpl(
            source,
            annotations.toMutableOrEmpty(),
            customRenderer,
            typeUseOrigin,
            innerTypeRef,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildParenthesizedTypeRef(init: CfirParenthesizedTypeRefBuilder.() -> Unit): CfirParenthesizedTypeRef {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirParenthesizedTypeRefBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildParenthesizedTypeRefCopy(original: CfirParenthesizedTypeRef, init: CfirParenthesizedTypeRefBuilder.() -> Unit): CfirParenthesizedTypeRef {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirParenthesizedTypeRefBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.customRenderer = original.customRenderer
    copyBuilder.typeUseOrigin = original.typeUseOrigin
    copyBuilder.innerTypeRef = original.innerTypeRef
    return copyBuilder.apply(init).build()
}
