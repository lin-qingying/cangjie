

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.types.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.types.CfirFunctionTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.impl.CfirFunctionTypeRefImpl
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirFunctionTypeRefBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    val parameterTypeRefs: MutableList<CfirTypeRef> = mutableListOf()
    lateinit var returnTypeRef: CfirTypeRef

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirFunctionTypeRef {
        return CfirFunctionTypeRefImpl(
            source,
            annotations.toMutableOrEmpty(),
            parameterTypeRefs,
            returnTypeRef,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildFunctionTypeRef(init: CfirFunctionTypeRefBuilder.() -> Unit): CfirFunctionTypeRef {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirFunctionTypeRefBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildFunctionTypeRefCopy(original: CfirFunctionTypeRef, init: CfirFunctionTypeRefBuilder.() -> Unit): CfirFunctionTypeRef {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirFunctionTypeRefBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.parameterTypeRefs.addAll(original.parameterTypeRefs)
    copyBuilder.returnTypeRef = original.returnTypeRef
    return copyBuilder.apply(init).build()
}
