

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirOptionalExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirOptionalExpressionImpl
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirOptionalExpressionBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangJieType? = null
    lateinit var expression: CfirExpression

    fun build(): CfirOptionalExpression {
        return CfirOptionalExpressionImpl(
            source,
            annotations.toMutableOrEmpty(),
            coneTypeOrNull,
            expression,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildOptionalExpression(init: CfirOptionalExpressionBuilder.() -> Unit): CfirOptionalExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirOptionalExpressionBuilder().apply(init).build()
}
