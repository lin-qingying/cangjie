

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirQuoteExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirQuoteExpressionImpl
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirQuoteExpressionBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangJieType? = null
    lateinit var rawText: String
    val interpolations: MutableList<CfirExpression> = mutableListOf()

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirQuoteExpression {
        return CfirQuoteExpressionImpl(
            source,
            annotations.toMutableOrEmpty(),
            coneTypeOrNull,
            rawText,
            interpolations,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildQuoteExpression(init: CfirQuoteExpressionBuilder.() -> Unit): CfirQuoteExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirQuoteExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildQuoteExpressionCopy(original: CfirQuoteExpression, init: CfirQuoteExpressionBuilder.() -> Unit): CfirQuoteExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirQuoteExpressionBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.rawText = original.rawText
    copyBuilder.interpolations.addAll(original.interpolations)
    return copyBuilder.apply(init).build()
}
