

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
import org.cangnova.cangjie.cfir.expressions.CfirNativeConditionMatchCase
import org.cangnova.cangjie.cfir.expressions.CfirNativeConditionMatchExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirNativeConditionMatchExpressionImpl
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirNativeConditionMatchExpressionBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangJieType? = null
    var mappedExpression: CfirExpression? = null
    var overflowStrategy: CangjieOverflowStrategy? = null
    val cases: MutableList<CfirNativeConditionMatchCase> = mutableListOf()

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirNativeConditionMatchExpression {
        return CfirNativeConditionMatchExpressionImpl(
            source,
            annotations.toMutableOrEmpty(),
            coneTypeOrNull,
            mappedExpression,
            overflowStrategy,
            cases,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildNativeConditionMatchExpression(init: CfirNativeConditionMatchExpressionBuilder.() -> Unit = {}): CfirNativeConditionMatchExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirNativeConditionMatchExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildNativeConditionMatchExpressionCopy(original: CfirNativeConditionMatchExpression, init: CfirNativeConditionMatchExpressionBuilder.() -> Unit = {}): CfirNativeConditionMatchExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirNativeConditionMatchExpressionBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.mappedExpression = original.mappedExpression
    copyBuilder.overflowStrategy = original.overflowStrategy
    copyBuilder.cases.addAll(original.cases)
    return copyBuilder.apply(init).build()
}
