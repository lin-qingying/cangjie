

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.types.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.declarations.CfirAnnotation
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.impl.CfirImplicitTypeRefImpl

@CfirBuilderDsl
class CfirImplicitTypeRefBuilder {
    val annotations: MutableList<CfirAnnotation> = mutableListOf()

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirImplicitTypeRef {
        return CfirImplicitTypeRefImpl(
            annotations,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildImplicitTypeRef(init: CfirImplicitTypeRefBuilder.() -> Unit = {}): CfirImplicitTypeRef {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirImplicitTypeRefBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildImplicitTypeRefCopy(original: CfirImplicitTypeRef, init: CfirImplicitTypeRefBuilder.() -> Unit = {}): CfirImplicitTypeRef {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirImplicitTypeRefBuilder()
    copyBuilder.annotations.addAll(original.annotations)
    return copyBuilder.apply(init).build()
}
