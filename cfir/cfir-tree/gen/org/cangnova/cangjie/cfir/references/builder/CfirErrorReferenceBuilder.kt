

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.references.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.references.CfirErrorReference
import org.cangnova.cangjie.cfir.references.impl.CfirErrorReferenceImpl
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirErrorReferenceBuilder {
    var source: CjSourceElement? = null
    lateinit var reason: String

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirErrorReference {
        return CfirErrorReferenceImpl(
            source,
            reason,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildErrorReference(init: CfirErrorReferenceBuilder.() -> Unit): CfirErrorReference {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirErrorReferenceBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildErrorReferenceCopy(original: CfirErrorReference, init: CfirErrorReferenceBuilder.() -> Unit): CfirErrorReference {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirErrorReferenceBuilder()
    copyBuilder.source = original.source
    copyBuilder.reason = original.reason
    return copyBuilder.apply(init).build()
}
