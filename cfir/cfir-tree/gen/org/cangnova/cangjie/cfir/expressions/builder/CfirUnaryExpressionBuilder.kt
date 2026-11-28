

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
import org.cangnova.cangjie.cfir.expressions.CfirUnaryExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirUnaryExpressionImpl
import org.cangnova.cangjie.cfir.source.CfirSourceOperator
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirUnaryExpressionBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangJieType? = null
    var mappedExpression: CfirExpression? = null
    var overflowStrategy: CangjieOverflowStrategy? = null
    lateinit var operand: CfirExpression
    lateinit var operation: CfirSourceOperator

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirUnaryExpression {
        return CfirUnaryExpressionImpl(
            source,
            annotations.toMutableOrEmpty(),
            coneTypeOrNull,
            mappedExpression,
            overflowStrategy,
            operand,
            operation,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildUnaryExpression(init: CfirUnaryExpressionBuilder.() -> Unit): CfirUnaryExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirUnaryExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildUnaryExpressionCopy(original: CfirUnaryExpression, init: CfirUnaryExpressionBuilder.() -> Unit): CfirUnaryExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirUnaryExpressionBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.mappedExpression = original.mappedExpression
    copyBuilder.overflowStrategy = original.overflowStrategy
    copyBuilder.operand = original.operand
    copyBuilder.operation = original.operation
    return copyBuilder.apply(init).build()
}
