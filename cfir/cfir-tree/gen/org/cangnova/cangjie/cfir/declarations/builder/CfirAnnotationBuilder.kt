

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.declarations.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.declarations.CfirAnnotation
import org.cangnova.cangjie.cfir.declarations.impl.CfirAnnotationImpl
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.types.CfirTypeRef

@CfirBuilderDsl
class CfirAnnotationBuilder {
    var source: CjSourceElement? = null
    lateinit var typeRef: CfirTypeRef
    val arguments: MutableList<CfirElement> = mutableListOf()

    fun build(): CfirAnnotation {
        return CfirAnnotationImpl(
            source,
            typeRef,
            arguments,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildAnnotation(init: CfirAnnotationBuilder.() -> Unit): CfirAnnotation {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirAnnotationBuilder().apply(init).build()
}
