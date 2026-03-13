

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.declarations.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.declarations.CfirAnnotation
import org.cangnova.cangjie.cfir.declarations.impl.CfirAnnotationImpl
import org.cangnova.cangjie.cfir.types.CfirTypeRef

@CfirBuilderDsl
class CfirAnnotationBuilder {
    lateinit var typeRef: CfirTypeRef
    val arguments: MutableList<CfirElement> = mutableListOf()

    fun build(): CfirAnnotation {
        return CfirAnnotationImpl(
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
