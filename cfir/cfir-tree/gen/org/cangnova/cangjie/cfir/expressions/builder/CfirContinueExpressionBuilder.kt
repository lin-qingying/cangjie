

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.CfirTarget
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirContinueExpression
import org.cangnova.cangjie.cfir.expressions.CfirLoopExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirContinueExpressionImpl
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirContinueExpressionBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangJieType? = null
    lateinit var target: CfirTarget<CfirLoopExpression>

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirContinueExpression {
        return CfirContinueExpressionImpl(
            source,
            annotations.toMutableOrEmpty(),
            coneTypeOrNull,
            target,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildContinueExpression(init: CfirContinueExpressionBuilder.() -> Unit): CfirContinueExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirContinueExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildContinueExpressionCopy(original: CfirContinueExpression, init: CfirContinueExpressionBuilder.() -> Unit): CfirContinueExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirContinueExpressionBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.target = original.target
    return copyBuilder.apply(init).build()
}
