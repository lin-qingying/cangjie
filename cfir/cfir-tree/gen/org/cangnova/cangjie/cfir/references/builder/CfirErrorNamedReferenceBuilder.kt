

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.references.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.references.CfirErrorNamedReference
import org.cangnova.cangjie.cfir.references.impl.CfirErrorNamedReferenceImpl
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirErrorNamedReferenceBuilder {
    var source: CjSourceElement? = null
    lateinit var name: Name
    lateinit var diagnostic: ConeDiagnostic

    fun build(): CfirErrorNamedReference {
        return CfirErrorNamedReferenceImpl(
            source,
            name,
            diagnostic,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildErrorNamedReference(init: CfirErrorNamedReferenceBuilder.() -> Unit): CfirErrorNamedReference {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirErrorNamedReferenceBuilder().apply(init).build()
}
