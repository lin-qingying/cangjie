

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
import org.cangnova.cangjie.cfir.expressions.CfirCallExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionArgument
import org.cangnova.cangjie.cfir.expressions.impl.CfirCallExpressionImpl
import org.cangnova.cangjie.cfir.source.CfirSourceCallKind
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirCallExpressionBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangJieType? = null
    var mappedExpression: CfirExpression? = null
    var overflowStrategy: CangjieOverflowStrategy? = null
    lateinit var base: CfirExpression
    val arguments: MutableList<CfirFunctionArgument> = mutableListOf()
    lateinit var callKind: CfirSourceCallKind
    var hasSideEffect: Boolean by kotlin.properties.Delegates.notNull<Boolean>()

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirCallExpression {
        return CfirCallExpressionImpl(
            source,
            annotations.toMutableOrEmpty(),
            coneTypeOrNull,
            mappedExpression,
            overflowStrategy,
            base,
            arguments,
            callKind,
            hasSideEffect,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildCallExpression(init: CfirCallExpressionBuilder.() -> Unit): CfirCallExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirCallExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildCallExpressionCopy(original: CfirCallExpression, init: CfirCallExpressionBuilder.() -> Unit): CfirCallExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirCallExpressionBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.mappedExpression = original.mappedExpression
    copyBuilder.overflowStrategy = original.overflowStrategy
    copyBuilder.base = original.base
    copyBuilder.arguments.addAll(original.arguments)
    copyBuilder.callKind = original.callKind
    copyBuilder.hasSideEffect = original.hasSideEffect
    return copyBuilder.apply(init).build()
}
