

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.types.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.types.CfirTupleTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.impl.CfirTupleTypeRefImpl
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirTupleTypeRefBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    val elementTypeRefs: MutableList<CfirTypeRef> = mutableListOf()

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirTupleTypeRef {
        return CfirTupleTypeRefImpl(
            source,
            annotations.toMutableOrEmpty(),
            elementTypeRefs,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildTupleTypeRef(init: CfirTupleTypeRefBuilder.() -> Unit = {}): CfirTupleTypeRef {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirTupleTypeRefBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildTupleTypeRefCopy(original: CfirTupleTypeRef, init: CfirTupleTypeRefBuilder.() -> Unit = {}): CfirTupleTypeRef {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirTupleTypeRefBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.elementTypeRefs.addAll(original.elementTypeRefs)
    return copyBuilder.apply(init).build()
}
