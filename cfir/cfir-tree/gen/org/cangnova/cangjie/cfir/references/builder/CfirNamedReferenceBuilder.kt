

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.references.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.impl.CfirNamedReferenceImpl
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.name.Name

@CfirBuilderDsl
class CfirNamedReferenceBuilder {
    var source: CjSourceElement? = null
    lateinit var name: Name

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirNamedReference {
        return CfirNamedReferenceImpl(
            source,
            name,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildNamedReference(init: CfirNamedReferenceBuilder.() -> Unit): CfirNamedReference {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirNamedReferenceBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildNamedReferenceCopy(original: CfirNamedReference, init: CfirNamedReferenceBuilder.() -> Unit): CfirNamedReference {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirNamedReferenceBuilder()
    copyBuilder.source = original.source
    copyBuilder.name = original.name
    return copyBuilder.apply(init).build()
}
