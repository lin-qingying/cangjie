

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirIncrementDecrementExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirIncrementDecrementExpressionImpl
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirIncrementDecrementExpressionBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangJieType? = null
    var isPrefix: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    lateinit var operationName: Name
    lateinit var expression: CfirExpression
    var operationSource: CjSourceElement? = null

    fun build(): CfirIncrementDecrementExpression {
        return CfirIncrementDecrementExpressionImpl(
            source,
            annotations.toMutableOrEmpty(),
            coneTypeOrNull,
            isPrefix,
            operationName,
            expression,
            operationSource,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildIncrementDecrementExpression(init: CfirIncrementDecrementExpressionBuilder.() -> Unit): CfirIncrementDecrementExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirIncrementDecrementExpressionBuilder().apply(init).build()
}
