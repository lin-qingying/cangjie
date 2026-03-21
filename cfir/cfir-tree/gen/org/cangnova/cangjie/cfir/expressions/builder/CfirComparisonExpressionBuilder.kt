

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.declarations.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirComparisonExpression
import org.cangnova.cangjie.cfir.expressions.CfirComparisonOp
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirComparisonExpressionImpl
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirComparisonExpressionBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangjieType? = null
    lateinit var operation: CfirComparisonOp
    lateinit var left: CfirExpression
    lateinit var right: CfirExpression

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirComparisonExpression {
        return CfirComparisonExpressionImpl(
            source,
            annotations,
            coneTypeOrNull,
            operation,
            left,
            right,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildComparisonExpression(init: CfirComparisonExpressionBuilder.() -> Unit): CfirComparisonExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirComparisonExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildComparisonExpressionCopy(original: CfirComparisonExpression, init: CfirComparisonExpressionBuilder.() -> Unit): CfirComparisonExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirComparisonExpressionBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.operation = original.operation
    copyBuilder.left = original.left
    copyBuilder.right = original.right
    return copyBuilder.apply(init).build()
}
