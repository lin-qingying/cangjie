

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.declarations.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirTupleLiteral
import org.cangnova.cangjie.cfir.expressions.impl.CfirTupleLiteralImpl
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirTupleLiteralBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangjieType? = null
    val elements: MutableList<CfirExpression> = mutableListOf()

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirTupleLiteral {
        return CfirTupleLiteralImpl(
            source,
            annotations,
            coneTypeOrNull,
            elements,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildTupleLiteral(init: CfirTupleLiteralBuilder.() -> Unit = {}): CfirTupleLiteral {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirTupleLiteralBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildTupleLiteralCopy(original: CfirTupleLiteral, init: CfirTupleLiteralBuilder.() -> Unit = {}): CfirTupleLiteral {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirTupleLiteralBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.elements.addAll(original.elements)
    return copyBuilder.apply(init).build()
}
