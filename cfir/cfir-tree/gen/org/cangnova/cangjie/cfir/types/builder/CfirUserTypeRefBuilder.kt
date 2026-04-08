

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
import org.cangnova.cangjie.cfir.types.CfirUserTypeRef
import org.cangnova.cangjie.cfir.types.impl.CfirUserTypeRefImpl
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirUserTypeRefBuilder {
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    lateinit var source: CjSourceElement
    val qualifier: MutableList<Name> = mutableListOf()
    val typeArguments: MutableList<CfirTypeRef> = mutableListOf()

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirUserTypeRef {
        return CfirUserTypeRefImpl(
            annotations.toMutableOrEmpty(),
            source,
            qualifier,
            typeArguments.toMutableOrEmpty(),
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildUserTypeRef(init: CfirUserTypeRefBuilder.() -> Unit): CfirUserTypeRef {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirUserTypeRefBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildUserTypeRefCopy(original: CfirUserTypeRef, init: CfirUserTypeRefBuilder.() -> Unit): CfirUserTypeRef {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirUserTypeRefBuilder()
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.source = original.source
    copyBuilder.qualifier.addAll(original.qualifier)
    copyBuilder.typeArguments.addAll(original.typeArguments)
    return copyBuilder.apply(init).build()
}
