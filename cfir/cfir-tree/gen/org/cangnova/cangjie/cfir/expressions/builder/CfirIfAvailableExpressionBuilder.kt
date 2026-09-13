

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
import org.cangnova.cangjie.cfir.expressions.CfirIfAvailableExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirIfAvailableExpressionImpl
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirIfAvailableExpressionBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangJieType? = null
    lateinit var conditionName: Name
    var conditionArgumentSource: CjSourceElement? = null
    var conditionNameSource: CjSourceElement? = null
    var condition: CfirExpression? = null
    lateinit var thenBranch: CfirExpression
    lateinit var elseBranch: CfirExpression

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirIfAvailableExpression {
        return CfirIfAvailableExpressionImpl(
            source,
            annotations.toMutableOrEmpty(),
            coneTypeOrNull,
            conditionName,
            conditionArgumentSource,
            conditionNameSource,
            condition,
            thenBranch,
            elseBranch,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildIfAvailableExpression(init: CfirIfAvailableExpressionBuilder.() -> Unit): CfirIfAvailableExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirIfAvailableExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildIfAvailableExpressionCopy(original: CfirIfAvailableExpression, init: CfirIfAvailableExpressionBuilder.() -> Unit): CfirIfAvailableExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirIfAvailableExpressionBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.conditionName = original.conditionName
    copyBuilder.conditionArgumentSource = original.conditionArgumentSource
    copyBuilder.conditionNameSource = original.conditionNameSource
    copyBuilder.condition = original.condition
    copyBuilder.thenBranch = original.thenBranch
    copyBuilder.elseBranch = original.elseBranch
    return copyBuilder.apply(init).build()
}
