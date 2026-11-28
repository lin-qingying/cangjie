

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
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirSubscriptExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirSubscriptExpressionImpl
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirSubscriptExpressionBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangJieType? = null
    var mappedExpression: CfirExpression? = null
    var overflowStrategy: CangjieOverflowStrategy? = null
    lateinit var receiver: CfirExpression
    val indices: MutableList<CfirExpression> = mutableListOf()
    var isTupleAccess: Boolean = false
    var resolvedGetCall: CfirFunctionCall? = null
    var resolvedSetCall: CfirFunctionCall? = null

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirSubscriptExpression {
        return CfirSubscriptExpressionImpl(
            source,
            annotations.toMutableOrEmpty(),
            coneTypeOrNull,
            mappedExpression,
            overflowStrategy,
            receiver,
            indices,
            isTupleAccess,
            resolvedGetCall,
            resolvedSetCall,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildSubscriptExpression(init: CfirSubscriptExpressionBuilder.() -> Unit): CfirSubscriptExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirSubscriptExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildSubscriptExpressionCopy(original: CfirSubscriptExpression, init: CfirSubscriptExpressionBuilder.() -> Unit): CfirSubscriptExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirSubscriptExpressionBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.mappedExpression = original.mappedExpression
    copyBuilder.overflowStrategy = original.overflowStrategy
    copyBuilder.receiver = original.receiver
    copyBuilder.indices.addAll(original.indices)
    copyBuilder.isTupleAccess = original.isTupleAccess
    copyBuilder.resolvedGetCall = original.resolvedGetCall
    copyBuilder.resolvedSetCall = original.resolvedSetCall
    return copyBuilder.apply(init).build()
}
