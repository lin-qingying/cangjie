

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirNamedArgumentExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirNamedArgumentExpressionImpl
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirNamedArgumentExpressionBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangJieType? = null
    lateinit var expression: CfirExpression
    lateinit var argumentName: Name
    var nameSource: CjSourceElement? = null

    fun build(): CfirNamedArgumentExpression {
        return CfirNamedArgumentExpressionImpl(
            source,
            annotations.toMutableOrEmpty(),
            coneTypeOrNull,
            expression,
            argumentName,
            nameSource,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildNamedArgumentExpression(init: CfirNamedArgumentExpressionBuilder.() -> Unit): CfirNamedArgumentExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirNamedArgumentExpressionBuilder().apply(init).build()
}
