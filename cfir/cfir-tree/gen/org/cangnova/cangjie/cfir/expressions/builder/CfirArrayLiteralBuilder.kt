

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.annotations.CangjieOverflowStrategy
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirArrayLiteral
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirArrayLiteralImpl
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirArrayLiteralBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangJieType? = null
    var mappedExpression: CfirExpression? = null
    var overflowStrategy: CangjieOverflowStrategy? = null
    val elements: MutableList<CfirExpression> = mutableListOf()
    var initializerSymbol: CfirFunctionSymbol<*>? = null

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirArrayLiteral {
        return CfirArrayLiteralImpl(
            source,
            annotations.toMutableOrEmpty(),
            coneTypeOrNull,
            mappedExpression,
            overflowStrategy,
            elements,
            initializerSymbol,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildArrayLiteral(init: CfirArrayLiteralBuilder.() -> Unit = {}): CfirArrayLiteral {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirArrayLiteralBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildArrayLiteralCopy(original: CfirArrayLiteral, init: CfirArrayLiteralBuilder.() -> Unit = {}): CfirArrayLiteral {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirArrayLiteralBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.mappedExpression = original.mappedExpression
    copyBuilder.overflowStrategy = original.overflowStrategy
    copyBuilder.elements.addAll(original.elements)
    copyBuilder.initializerSymbol = original.initializerSymbol
    return copyBuilder.apply(init).build()
}
