

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
import org.cangnova.cangjie.cfir.expressions.CfirBinaryExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirBinaryExpressionImpl
import org.cangnova.cangjie.cfir.source.CfirSourceOperator
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirBinaryExpressionBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangJieType? = null
    var mappedExpression: CfirExpression? = null
    var overflowStrategy: CangjieOverflowStrategy? = null
    lateinit var left: CfirExpression
    lateinit var right: CfirExpression
    lateinit var operation: CfirSourceOperator

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirBinaryExpression {
        return CfirBinaryExpressionImpl(
            source,
            annotations.toMutableOrEmpty(),
            coneTypeOrNull,
            mappedExpression,
            overflowStrategy,
            left,
            right,
            operation,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildBinaryExpression(init: CfirBinaryExpressionBuilder.() -> Unit): CfirBinaryExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirBinaryExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildBinaryExpressionCopy(original: CfirBinaryExpression, init: CfirBinaryExpressionBuilder.() -> Unit): CfirBinaryExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirBinaryExpressionBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.mappedExpression = original.mappedExpression
    copyBuilder.overflowStrategy = original.overflowStrategy
    copyBuilder.left = original.left
    copyBuilder.right = original.right
    copyBuilder.operation = original.operation
    return copyBuilder.apply(init).build()
}
