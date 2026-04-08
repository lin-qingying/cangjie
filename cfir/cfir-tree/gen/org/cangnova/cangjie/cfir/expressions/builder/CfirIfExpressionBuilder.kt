

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirIfExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirIfExpressionImpl
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirIfExpressionBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangJieType? = null
    lateinit var condition: CfirExpression
    lateinit var thenBranch: CfirBlock
    var elseBranch: CfirExpression? = null

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirIfExpression {
        return CfirIfExpressionImpl(
            source,
            annotations.toMutableOrEmpty(),
            coneTypeOrNull,
            condition,
            thenBranch,
            elseBranch,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildIfExpression(init: CfirIfExpressionBuilder.() -> Unit): CfirIfExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirIfExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildIfExpressionCopy(original: CfirIfExpression, init: CfirIfExpressionBuilder.() -> Unit): CfirIfExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirIfExpressionBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.condition = original.condition
    copyBuilder.thenBranch = original.thenBranch
    copyBuilder.elseBranch = original.elseBranch
    return copyBuilder.apply(init).build()
}
