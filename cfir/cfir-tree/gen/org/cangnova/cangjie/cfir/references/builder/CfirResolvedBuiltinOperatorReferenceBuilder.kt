

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.references.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.references.CfirResolvedBuiltinOperatorReference
import org.cangnova.cangjie.cfir.references.impl.CfirResolvedBuiltinOperatorReferenceImpl
import org.cangnova.cangjie.cfir.types.BuiltinPrimitiveOperatorSignature
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirResolvedBuiltinOperatorReferenceBuilder {
    var source: CjSourceElement? = null
    lateinit var name: Name
    lateinit var signature: BuiltinPrimitiveOperatorSignature

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirResolvedBuiltinOperatorReference {
        return CfirResolvedBuiltinOperatorReferenceImpl(
            source,
            name,
            signature,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildResolvedBuiltinOperatorReference(init: CfirResolvedBuiltinOperatorReferenceBuilder.() -> Unit): CfirResolvedBuiltinOperatorReference {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirResolvedBuiltinOperatorReferenceBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildResolvedBuiltinOperatorReferenceCopy(original: CfirResolvedBuiltinOperatorReference, init: CfirResolvedBuiltinOperatorReferenceBuilder.() -> Unit): CfirResolvedBuiltinOperatorReference {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirResolvedBuiltinOperatorReferenceBuilder()
    copyBuilder.source = original.source
    copyBuilder.name = original.name
    copyBuilder.signature = original.signature
    return copyBuilder.apply(init).build()
}
