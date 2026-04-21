

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.types.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.types.CfirBasicTypeRef
import org.cangnova.cangjie.cfir.types.impl.CfirBasicTypeRefImpl
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirBasicTypeRefBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var customRenderer: Boolean = false
    lateinit var name: Name

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirBasicTypeRef {
        return CfirBasicTypeRefImpl(
            source,
            annotations.toMutableOrEmpty(),
            customRenderer,
            name,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildBasicTypeRef(init: CfirBasicTypeRefBuilder.() -> Unit): CfirBasicTypeRef {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirBasicTypeRefBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildBasicTypeRefCopy(original: CfirBasicTypeRef, init: CfirBasicTypeRefBuilder.() -> Unit): CfirBasicTypeRef {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirBasicTypeRefBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.customRenderer = original.customRenderer
    copyBuilder.name = original.name
    return copyBuilder.apply(init).build()
}
