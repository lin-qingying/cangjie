

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.types.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.declarations.CfirAnnotation
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.types.impl.CfirResolvedTypeRefImpl

@CfirBuilderDsl
class CfirResolvedTypeRefBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    lateinit var coneType: ConeCangjieType
    var delegatedTypeRef: CfirTypeRef? = null

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirResolvedTypeRef {
        return CfirResolvedTypeRefImpl(
            source,
            annotations,
            coneType,
            delegatedTypeRef,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildResolvedTypeRef(init: CfirResolvedTypeRefBuilder.() -> Unit): CfirResolvedTypeRef {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirResolvedTypeRefBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildResolvedTypeRefCopy(original: CfirResolvedTypeRef, init: CfirResolvedTypeRefBuilder.() -> Unit): CfirResolvedTypeRef {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirResolvedTypeRefBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneType = original.coneType
    copyBuilder.delegatedTypeRef = original.delegatedTypeRef
    return copyBuilder.apply(init).build()
}
