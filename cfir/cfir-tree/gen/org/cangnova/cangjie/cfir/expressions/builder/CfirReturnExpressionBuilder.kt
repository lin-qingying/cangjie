

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.CfirTarget
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirReturnExpressionImpl
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirReturnExpressionBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangJieType? = null
    lateinit var target: CfirTarget<CfirFunction>
    lateinit var result: CfirExpression

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirReturnExpression {
        return CfirReturnExpressionImpl(
            source,
            annotations.toMutableOrEmpty(),
            coneTypeOrNull,
            target,
            result,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildReturnExpression(init: CfirReturnExpressionBuilder.() -> Unit): CfirReturnExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirReturnExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildReturnExpressionCopy(original: CfirReturnExpression, init: CfirReturnExpressionBuilder.() -> Unit): CfirReturnExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirReturnExpressionBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.target = original.target
    copyBuilder.result = original.result
    return copyBuilder.apply(init).build()
}
