

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.references.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.references.CfirSuperReference
import org.cangnova.cangjie.cfir.references.impl.CfirSuperReferenceImpl
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirSuperReferenceBuilder {
    var source: CjSourceElement? = null
    lateinit var superTypeRef: CfirTypeRef

    fun build(): CfirSuperReference {
        return CfirSuperReferenceImpl(
            source,
            superTypeRef,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildSuperReference(init: CfirSuperReferenceBuilder.() -> Unit): CfirSuperReference {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirSuperReferenceBuilder().apply(init).build()
}
