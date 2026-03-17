

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.types.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.declarations.CfirAnnotation
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.impl.CfirErrorTypeRefImpl

@CfirBuilderDsl
class CfirErrorTypeRefBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    lateinit var reason: String

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirErrorTypeRef {
        return CfirErrorTypeRefImpl(
            source,
            annotations,
            reason,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildErrorTypeRef(init: CfirErrorTypeRefBuilder.() -> Unit): CfirErrorTypeRef {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirErrorTypeRefBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildErrorTypeRefCopy(original: CfirErrorTypeRef, init: CfirErrorTypeRefBuilder.() -> Unit): CfirErrorTypeRef {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirErrorTypeRefBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.reason = original.reason
    return copyBuilder.apply(init).build()
}
