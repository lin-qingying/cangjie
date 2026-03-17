

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.declarations.CfirAnnotation
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.expressions.CfirLambdaExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirLambdaExpressionImpl
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.types.ConeCangjieType

@CfirBuilderDsl
class CfirLambdaExpressionBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangjieType? = null
    lateinit var anonymousFunction: CfirFunction

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirLambdaExpression {
        return CfirLambdaExpressionImpl(
            source,
            annotations,
            coneTypeOrNull,
            anonymousFunction,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildLambdaExpression(init: CfirLambdaExpressionBuilder.() -> Unit): CfirLambdaExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirLambdaExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildLambdaExpressionCopy(original: CfirLambdaExpression, init: CfirLambdaExpressionBuilder.() -> Unit): CfirLambdaExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirLambdaExpressionBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.anonymousFunction = original.anonymousFunction
    return copyBuilder.apply(init).build()
}
