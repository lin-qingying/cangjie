

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirForInExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirForInExpressionImpl
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirForInExpressionBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangJieType? = null
    lateinit var condition: CfirExpression
    var isDoWhile: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    lateinit var variable: CfirPatternVariable
    lateinit var iterable: CfirExpression
    lateinit var body: CfirBlock

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirForInExpression {
        return CfirForInExpressionImpl(
            source,
            annotations.toMutableOrEmpty(),
            coneTypeOrNull,
            condition,
            isDoWhile,
            variable,
            iterable,
            body,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildForInExpression(init: CfirForInExpressionBuilder.() -> Unit): CfirForInExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirForInExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildForInExpressionCopy(original: CfirForInExpression, init: CfirForInExpressionBuilder.() -> Unit): CfirForInExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirForInExpressionBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.condition = original.condition
    copyBuilder.isDoWhile = original.isDoWhile
    copyBuilder.variable = original.variable
    copyBuilder.iterable = original.iterable
    copyBuilder.body = original.body
    return copyBuilder.apply(init).build()
}
