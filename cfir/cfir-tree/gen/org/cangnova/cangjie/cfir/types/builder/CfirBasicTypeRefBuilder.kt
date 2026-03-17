

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.types.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.declarations.CfirAnnotation
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.types.CfirBasicTypeRef
import org.cangnova.cangjie.cfir.types.impl.CfirBasicTypeRefImpl
import org.cangnova.cangjie.name.Name

@CfirBuilderDsl
class CfirBasicTypeRefBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    lateinit var name: Name

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirBasicTypeRef {
        return CfirBasicTypeRefImpl(
            source,
            annotations,
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
    copyBuilder.name = original.name
    return copyBuilder.apply(init).build()
}
