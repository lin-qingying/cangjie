

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.types.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.impl.CfirImplicitTypeRefImpl

@CfirBuilderDsl
class CfirImplicitTypeRefBuilder {
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var customRenderer: Boolean = false

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirImplicitTypeRef {
        return CfirImplicitTypeRefImpl(
            annotations.toMutableOrEmpty(),
            customRenderer,
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
    copyBuilder.customRenderer = original.customRenderer
    return copyBuilder.apply(init).build()
}
