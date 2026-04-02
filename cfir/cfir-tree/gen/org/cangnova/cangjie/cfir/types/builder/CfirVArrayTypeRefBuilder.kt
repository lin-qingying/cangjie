

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.types.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.CfirVArrayTypeRef
import org.cangnova.cangjie.cfir.types.impl.CfirVArrayTypeRefImpl
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirVArrayTypeRefBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    lateinit var elementTypeRef: CfirTypeRef
    lateinit var sizeLiteral: String

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirVArrayTypeRef {
        return CfirVArrayTypeRefImpl(
            source,
            annotations.toMutableOrEmpty(),
            elementTypeRef,
            sizeLiteral,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildVArrayTypeRef(init: CfirVArrayTypeRefBuilder.() -> Unit): CfirVArrayTypeRef {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirVArrayTypeRefBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildVArrayTypeRefCopy(original: CfirVArrayTypeRef, init: CfirVArrayTypeRefBuilder.() -> Unit): CfirVArrayTypeRef {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirVArrayTypeRefBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.elementTypeRef = original.elementTypeRef
    copyBuilder.sizeLiteral = original.sizeLiteral
    return copyBuilder.apply(init).build()
}
