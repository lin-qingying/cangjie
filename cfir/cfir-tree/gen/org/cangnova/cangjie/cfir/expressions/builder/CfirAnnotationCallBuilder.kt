

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirArgumentList
import org.cangnova.cangjie.cfir.expressions.impl.CfirAnnotationCallImpl
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirAnnotationCallBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangJieType? = null
    lateinit var typeRef: CfirTypeRef
    val arguments: MutableList<CfirElement> = mutableListOf()
    lateinit var argumentList: CfirArgumentList
    lateinit var calleeReference: CfirReference
    lateinit var containingDeclarationSymbol: CfirBasedSymbol<*>

    fun build(): CfirAnnotationCall {
        return CfirAnnotationCallImpl(
            source,
            annotations.toMutableOrEmpty(),
            coneTypeOrNull,
            typeRef,
            arguments,
            argumentList,
            calleeReference,
            containingDeclarationSymbol,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildAnnotationCall(init: CfirAnnotationCallBuilder.() -> Unit): CfirAnnotationCall {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirAnnotationCallBuilder().apply(init).build()
}
