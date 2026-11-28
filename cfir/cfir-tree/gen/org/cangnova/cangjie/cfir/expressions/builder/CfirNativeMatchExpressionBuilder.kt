

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
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirNativeMatchCase
import org.cangnova.cangjie.cfir.expressions.CfirNativeMatchExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirNativeMatchExpressionImpl
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirNativeMatchExpressionBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangJieType? = null
    var mappedExpression: CfirExpression? = null
    var overflowStrategy: CangjieOverflowStrategy? = null
    lateinit var selector: CfirExpression
    val cases: MutableList<CfirNativeMatchCase> = mutableListOf()

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirNativeMatchExpression {
        return CfirNativeMatchExpressionImpl(
            source,
            annotations.toMutableOrEmpty(),
            coneTypeOrNull,
            mappedExpression,
            overflowStrategy,
            selector,
            cases,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildNativeMatchExpression(init: CfirNativeMatchExpressionBuilder.() -> Unit): CfirNativeMatchExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirNativeMatchExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildNativeMatchExpressionCopy(original: CfirNativeMatchExpression, init: CfirNativeMatchExpressionBuilder.() -> Unit): CfirNativeMatchExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirNativeMatchExpressionBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.mappedExpression = original.mappedExpression
    copyBuilder.overflowStrategy = original.overflowStrategy
    copyBuilder.selector = original.selector
    copyBuilder.cases.addAll(original.cases)
    return copyBuilder.apply(init).build()
}
